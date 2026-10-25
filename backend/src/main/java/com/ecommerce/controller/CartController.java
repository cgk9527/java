package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.common.util.UserContext;
import com.ecommerce.dto.CartAddRequest;
import com.ecommerce.dto.CartItemVO;
import com.ecommerce.service.CartService;
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

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Result<List<CartItemVO>> list() {
        return Result.success(cartService.list(UserContext.getUserId()));
    }

    @GetMapping("/count")
    public Result<Long> count() {
        return Result.success(cartService.count(UserContext.getUserId()));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody CartAddRequest request) {
        cartService.add(UserContext.getUserId(), request.getSkuId(), request.getQuantity());
        return Result.success();
    }

    @PutMapping("/{skuId}")
    public Result<Void> update(@PathVariable Long skuId, @RequestParam int quantity) {
        cartService.update(UserContext.getUserId(), skuId, quantity);
        return Result.success();
    }

    @DeleteMapping("/{skuId}")
    public Result<Void> delete(@PathVariable Long skuId) {
        cartService.delete(UserContext.getUserId(), skuId);
        return Result.success();
    }

    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(UserContext.getUserId());
        return Result.success();
    }
}