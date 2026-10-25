package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.common.util.UserContext;
import com.ecommerce.dto.OrderCreateRequest;
import com.ecommerce.dto.OrderVO;
import com.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.createOrder(UserContext.getUserId(), request));
    }

    @GetMapping
    public Result<List<OrderVO>> list() {
        return Result.success(orderService.listByUser(UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(UserContext.getUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(UserContext.getUserId(), id);
        return Result.success();
    }
}