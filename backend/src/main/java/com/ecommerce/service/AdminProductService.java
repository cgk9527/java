package com.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ecommerce.common.ResultCode;
import com.ecommerce.common.exception.BizException;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.SkuRequest;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Sku;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.mapper.SkuMapper;
import com.ecommerce.mq.ProductSyncProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品后台管理服务(增删改商品/SKU 并同步 ES)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final StockService stockService;
    private final ProductSyncProducer productSyncProducer;

    public List<Product> list() {
        return productMapper.selectList(
                Wrappers.<Product>lambdaQuery().orderByDesc(Product::getId));
    }

    public List<Sku> listSkus(Long productId) {
        return skuMapper.selectList(Wrappers.<Sku>lambdaQuery()
                .eq(Sku::getProductId, productId)
                .orderByAsc(Sku::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ProductRequest request) {
        Product product = new Product();
        applyProduct(product, request);
        product.setSales(0L);
        product.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        productMapper.insert(product);

        if (request.getSkus() != null) {
            for (SkuRequest skuRequest : request.getSkus()) {
                insertSku(product.getId(), skuRequest);
            }
        }
        productSyncProducer.send(product.getId());
        return product.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        applyProduct(product, request);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        productSyncProducer.send(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Product product = getProduct(id);
        product.setStatus(0);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        productSyncProducer.send(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Product product = getProduct(id);
        product.setStatus(status);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
        productSyncProducer.send(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long addSku(Long productId, SkuRequest request) {
        getProduct(productId);
        Long skuId = insertSku(productId, request);
        refreshProductPrice(productId);
        return skuId;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateSku(Long skuId, SkuRequest request) {
        Sku sku = getSku(skuId);
        sku.setSkuCode(request.getSkuCode());
        sku.setSpecs(request.getSpecs());
        sku.setPrice(request.getPrice());
        sku.setStock(request.getStock());
        sku.setUpdatedAt(LocalDateTime.now());
        skuMapper.updateById(sku);
        // 库存变更后重新同步 Redis
        stockService.initStock(skuId);
        refreshProductPrice(sku.getProductId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSku(Long skuId) {
        Sku sku = getSku(skuId);
        sku.setStatus(0);
        sku.setUpdatedAt(LocalDateTime.now());
        skuMapper.updateById(sku);
        refreshProductPrice(sku.getProductId());
    }

    private Long insertSku(Long productId, SkuRequest request) {
        Sku sku = new Sku();
        sku.setProductId(productId);
        sku.setSkuCode(request.getSkuCode());
        sku.setSpecs(request.getSpecs());
        sku.setPrice(request.getPrice());
        sku.setStock(request.getStock());
        sku.setSales(0L);
        sku.setStatus(1);
        sku.setVersion(0L);
        LocalDateTime now = LocalDateTime.now();
        sku.setCreatedAt(now);
        sku.setUpdatedAt(now);
        skuMapper.insert(sku);
        stockService.initStock(sku.getId());
        return sku.getId();
    }

    /**
     * 以最低在售 SKU 价格刷新商品展示价, 并同步 ES
     */
    private void refreshProductPrice(Long productId) {
        List<Sku> skus = skuMapper.selectList(Wrappers.<Sku>lambdaQuery()
                .eq(Sku::getProductId, productId)
                .eq(Sku::getStatus, 1));
        if (skus.isEmpty()) {
            return;
        }
        BigDecimal minPrice = skus.stream().map(Sku::getPrice).min(BigDecimal::compareTo).orElse(null);
        Product product = productMapper.selectById(productId);
        if (product != null && minPrice != null) {
            product.setPrice(minPrice);
            product.setUpdatedAt(LocalDateTime.now());
            productMapper.updateById(product);
        }
        productSyncProducer.send(productId);
    }

    private void applyProduct(Product product, ProductRequest request) {
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName());
        product.setSubtitle(request.getSubtitle());
        product.setMainImage(request.getMainImage());
        product.setSubImages(request.getSubImages());
        product.setDetail(request.getDetail());
        product.setPrice(request.getPrice());
    }

    private Product getProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return product;
    }

    private Sku getSku(Long id) {
        Sku sku = skuMapper.selectById(id);
        if (sku == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return sku;
    }
}