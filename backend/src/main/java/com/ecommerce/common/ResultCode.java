package com.ecommerce.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务响应码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),

    USERNAME_EXISTS(1001, "用户名已存在"),
    USERNAME_OR_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_DISABLED(1003, "账号已被禁用"),

    STOCK_NOT_ENOUGH(2001, "商品库存不足"),
    PRODUCT_NOT_ON_SALE(2002, "商品已下架"),
    ORDER_NOT_FOUND(2003, "订单不存在"),
    ORDER_STATUS_ERROR(2004, "订单状态不允许该操作"),
    CART_EMPTY(2005, "购物车为空"),

    SYSTEM_ERROR(5000, "系统繁忙, 请稍后重试");

    private final int code;
    private final String message;
}