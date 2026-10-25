package com.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.PageResult;
import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.dto.ReviewRequest;
import com.ecommerce.dto.ReviewVO;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Review;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.OrderItemMapper;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.ReviewMapper;
import com.ecommerce.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品评价服务
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;

    @Transactional(rollbackFor = Exception.class)
    public void create(Long userId, Long productId, ReviewRequest request) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!hasCompletedPurchase(userId, productId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "购买并完成订单后才能评价");
        }
        Review review = new Review();
        review.setProductId(productId);
        review.setUserId(userId);
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setStatus(1);
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);
    }

    public PageResult<ReviewVO> list(Long productId, int page, int size) {
        Page<Review> result = reviewMapper.selectPage(new Page<>(page, size),
                Wrappers.<Review>lambdaQuery()
                        .eq(Review::getProductId, productId)
                        .eq(Review::getStatus, 1)
                        .orderByDesc(Review::getId));
        List<ReviewVO> records = result.getRecords().stream().map(review -> {
            ReviewVO vo = new ReviewVO();
            vo.setId(review.getId());
            vo.setUserId(review.getUserId());
            vo.setRating(review.getRating());
            vo.setContent(review.getContent());
            vo.setCreatedAt(review.getCreatedAt());
            User user = userMapper.selectById(review.getUserId());
            vo.setNickname(user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "匿名");
            return vo;
        }).toList();
        return PageResult.of(result.getTotal(), page, size, records);
    }

    private boolean hasCompletedPurchase(Long userId, Long productId) {
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getProductId, productId));
        if (items.isEmpty()) {
            return false;
        }
        List<Long> orderIds = items.stream().map(OrderItem::getOrderId).distinct().toList();
        Long count = orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, Order.STATUS_COMPLETED)
                .in(Order::getId, orderIds));
        return count != null && count > 0;
    }
}