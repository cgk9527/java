package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.common.util.UserContext;
import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.SkuRequest;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Sku;
import com.ecommerce.service.AdminProductService;
import com.ecommerce.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品后台管理接口(仅管理员)
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminProductController {

    private final UserService userService;
    private final AdminProductService adminProductService;

    private void checkAdmin() {
        userService.requireAdmin(UserContext.getUserId());
    }

    @GetMapping("/products")
    public Result<List<Product>> list() {
        checkAdmin();
        return Result.success(adminProductService.list());
    }

    @PostMapping("/products")
    public Result<Long> create(@Valid @RequestBody ProductRequest request) {
        checkAdmin();
        return Result.success(adminProductService.create(request));
    }

    @GetMapping("/products/{productId}/skus")
    public Result<List<Sku>> listSkus(@PathVariable Long productId) {
        checkAdmin();
        return Result.success(adminProductService.listSkus(productId));
    }

    @PutMapping("/products/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        checkAdmin();
        adminProductService.update(id, request);
        return Result.success();
    }

    @DeleteMapping("/products/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        adminProductService.delete(id);
        return Result.success();
    }

    @PutMapping("/products/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        checkAdmin();
        adminProductService.updateStatus(id, status);
        return Result.success();
    }

    @PostMapping("/products/{productId}/skus")
    public Result<Long> addSku(@PathVariable Long productId, @Valid @RequestBody SkuRequest request) {
        checkAdmin();
        return Result.success(adminProductService.addSku(productId, request));
    }

    @PutMapping("/skus/{skuId}")
    public Result<Void> updateSku(@PathVariable Long skuId, @Valid @RequestBody SkuRequest request) {
        checkAdmin();
        adminProductService.updateSku(skuId, request);
        return Result.success();
    }

    @DeleteMapping("/skus/{skuId}")
    public Result<Void> deleteSku(@PathVariable Long skuId) {
        checkAdmin();
        adminProductService.deleteSku(skuId);
        return Result.success();
    }
}