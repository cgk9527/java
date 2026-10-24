package com.ecommerce.interceptor;

import com.ecommerce.common.exception.BizException;
import com.ecommerce.common.ResultCode;
import com.ecommerce.common.util.JwtUtil;
import com.ecommerce.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权拦截器(解析 JWT 并写入用户上下文)
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader(header);
        if (!StringUtils.hasText(token) || !token.startsWith(prefix)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        String rawToken = token.substring(prefix.length()).trim();
        try {
            Long userId = jwtUtil.parseUserId(rawToken);
            UserContext.setUserId(userId);
            return true;
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}