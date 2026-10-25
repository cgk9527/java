package com.ecommerce.common.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * 简单 HTTP 客户端(用于支付宝/微信服务端接口调用)
 */
public final class HttpUtil {

    private static final HttpClient CLIENT = HttpClient.newBuilder().build();

    private HttpUtil() {
    }

    public static String post(String url, String body, String contentType) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.body();
        } catch (Exception e) {
            throw new IllegalStateException("HTTP 请求失败: " + e.getMessage(), e);
        }
    }
}