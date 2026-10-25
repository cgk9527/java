package com.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.dto.OrderCreateRequest;
import com.ecommerce.dto.OrderItemRequest;
import com.ecommerce.dto.OrderItemVO;
import com.ecommerce.dto.OrderVO;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Sku;
import com.ecommerce.event.OrderStockChangedEvent;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.SkuMapper;
import com.ecommerce.mq.OrderStockMessage;
import com.ecommerce.mq.OrderTimeoutProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务(创建订单: Redis 预扣库存 -> 落库 -> 事务提交后 MQ 异步落库)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final SkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final StockService stockService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderTimeoutProducer orderTimeoutProducer;

    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, OrderCreateRequest request) {
        List<Long> skuIds = request.getItems().stream()
                .map(OrderItemRequest::getSkuId).distinct().toList();
        List<Sku> skus = skuMapper.selectBatchIds(skuIds);
        if (skus.size() != skuIds.size()) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        Map<Long, Sku> skuMap = skus.stream().collect(Collectors.toMap(Sku::getId, Function.identity()));

        // 预扣库存 + 构造订单项快照
        List<OrderStockMessage.Item> stockItems = new ArrayList<>();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        try {
            for (OrderItemRequest item : request.getItems()) {
                Sku sku = skuMap.get(item.getSkuId());
                if (sku.getStatus() == null || sku.getStatus() != 1) {
                    throw new BizException(ResultCode.PRODUCT_NOT_ON_SALE);
                }
                Product product = productMapper.selectById(sku.getProductId());
                if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                    throw new BizException(ResultCode.PRODUCT_NOT_ON_SALE);
                }

                stockService.deduct(item.getSkuId(), item.getQuantity());
                stockItems.add(new OrderStockMessage.Item(
                        sku.getId(), sku.getProductId(), item.getQuantity()));

                BigDecimal itemTotal = sku.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);

                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setSkuId(sku.getId());
                orderItem.setProductName(product.getName());
                orderItem.setSkuSpecs(sku.getSpecs());
                orderItem.setProductImage(product.getMainImage());
                orderItem.setPrice(sku.getPrice());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setTotalAmount(itemTotal);
                orderItems.add(orderItem);
            }
        } catch (BizException e) {
            // 回补已预扣库存
            stockItems.forEach(i -> stockService.restore(i.getSkuId(), i.getQuantity()));
            throw e;
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo(userId));
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);
        order.setStatus(Order.STATUS_PENDING_PAY);
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        order.setRemark(request.getRemark());
        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        try {
            orderMapper.insert(order);
            orderItems.forEach(item -> {
                item.setOrderId(order.getId());
                orderItemMapper.insert(item);
            });
        } catch (Exception e) {
            log.error("订单落库失败", e);
            stockItems.forEach(i -> stockService.restore(i.getSkuId(), i.getQuantity()));
            throw new BizException(ResultCode.SYSTEM_ERROR);
        }

        // 发布事件, 事务提交后投递 MQ 异步落库库存
        OrderStockMessage message = OrderStockMessage.builder()
                .operation(OrderStockMessage.OPERATION_DEDUCT)
                .orderId(order.getId())
                .userId(userId)
                .items(stockItems)
                .build();
        eventPublisher.publishEvent(new OrderStockChangedEvent(this, message));

        // 投递延时消息, 超时未支付自动关闭订单
        orderTimeoutProducer.send(order.getId());

        return toVO(order, orderItems);
    }

    public List<OrderVO> listByUser(Long userId) {
        List<Order> orders = orderMapper.selectList(
                Wrappers.<Order>lambdaQuery()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreatedAt));
        return orders.stream().map(order -> toVO(order, findItems(order.getId()))).toList();
    }

    public OrderVO detail(Long userId, Long orderId) {
        Order order = getOwnOrder(userId, orderId);
        return toVO(order, findItems(orderId));
    }

    /**
     * 查询订单状态(供支付结果轮询)
     */
    public Integer getStatus(Long userId, Long orderId) {
        return getOwnOrder(userId, orderId).getStatus();
    }

    /**
     * 按订单号查询状态(供支付宝 return_url 回调后轮询)
     */
    public Integer getStatusByOrderNo(Long userId, String orderNo) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order.getStatus();
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId) {
        Order order = getOwnOrder(userId, orderId);
        if (order.getStatus() != Order.STATUS_PENDING_PAY) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }
        doCancel(order);
    }

    /**
     * 订单超时自动关闭(由 MQ 消费者触发, 幂等)
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoCancel(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != Order.STATUS_PENDING_PAY) {
            return;
        }
        doCancel(order);
    }

    private void doCancel(Order order) {
        order.setStatus(Order.STATUS_CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        List<OrderItem> items = findItems(order.getId());
        items.forEach(item -> stockService.restore(item.getSkuId(), item.getQuantity()));

        List<OrderStockMessage.Item> stockItems = items.stream()
                .map(i -> new OrderStockMessage.Item(i.getSkuId(), i.getProductId(), i.getQuantity()))
                .toList();
        OrderStockMessage message = OrderStockMessage.builder()
                .operation(OrderStockMessage.OPERATION_RESTORE)
                .orderId(order.getId())
                .userId(order.getUserId())
                .items(stockItems)
                .build();
        eventPublisher.publishEvent(new OrderStockChangedEvent(this, message));
    }

    private Order getOwnOrder(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private List<OrderItem> findItems(Long orderId) {
        return orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId));
    }

    private OrderVO toVO(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setRemark(order.getRemark());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setPaidAt(order.getPaidAt());
        vo.setItems(items.stream().map(item -> {
            OrderItemVO iv = new OrderItemVO();
            iv.setProductId(item.getProductId());
            iv.setSkuId(item.getSkuId());
            iv.setProductName(item.getProductName());
            iv.setSkuSpecs(item.getSkuSpecs());
            iv.setProductImage(item.getProductImage());
            iv.setPrice(item.getPrice());
            iv.setQuantity(item.getQuantity());
            iv.setTotalAmount(item.getTotalAmount());
            return iv;
        }).toList());
        return vo;
    }

    private String generateOrderNo(Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return timestamp + userId + random;
    }
}