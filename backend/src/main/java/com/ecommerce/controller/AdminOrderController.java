package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.common.util.UserContext;
import com.ecommerce.dto.OrderVO;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单后台管理(发货)
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final UserService userService;

    @GetMapping
    public Result<List<OrderVO>> list() {
        userService.requireAdmin(UserContext.getUserId());
        return Result.success(orderService.listAll());
    }

    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id,
                             @RequestParam(required = false) String trackingNo) {
        userService.requireAdmin(UserContext.getUserId());
        orderService.ship(id, trackingNo);
        return Result.success();
    }
}