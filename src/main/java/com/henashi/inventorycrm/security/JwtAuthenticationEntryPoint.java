package com.henashi.inventorycrm.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("认证失败: {} - {}", request.getServletPath(), authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 直接返回Map，不使用ApiResponse包装
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", authException.getMessage());
        errorResponse.put("path", request.getServletPath());

        // 添加自定义错误信息
        String errorDetail = getErrorDetail(authException);
        errorResponse.put("detail", errorDetail);

        // 序列化为JSON
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private String getErrorDetail(AuthenticationException exception) {
        String message = exception.getMessage();

        if (message.contains("JWT") || message.contains("token")) {
            return "Invalid or expired token";
        } else if (message.contains("credential")) {
            return "Invalid username or password";
        } else if (message.contains("disabled") || message.contains("locked")) {
            return "Account is disabled or locked";
        } else {
            return "Authentication failed";
        }
    }
}