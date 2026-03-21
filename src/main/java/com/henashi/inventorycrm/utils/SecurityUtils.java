package com.henashi.inventorycrm.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

/**
 * 安全工具类
 * 提供统一的安全相关操作
 */
@Component
@Slf4j
public class SecurityUtils {

    /**
     * 获取当前登录用户的用户名
     */
    public String getCurrentUsername() {
        Authentication authentication = getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("未获取到认证信息，使用系统默认用户");
            return "system";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        } else if (principal != null) {
            return principal.toString();
        }

        return "anonymous";
    }

    /**
     * 获取当前登录用户的ID
     */
    public Long getCurrentUserId() {
        Authentication authentication = getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // 从认证信息中提取用户ID
        // 这里假设你的用户信息中包含了用户ID
        Object details = authentication.getDetails();
        if (details instanceof CustomUserDetails) {
            return ((CustomUserDetails) details).getUserId();
        }

        return null;
    }

    /**
     * 获取当前登录用户的详细信息
     */
    public Optional<UserDetails> getCurrentUserDetails() {
        Authentication authentication = getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return Optional.of((UserDetails) principal);
        }

        return Optional.empty();
    }

    /**
     * 检查当前用户是否已认证
     */
    public boolean isAuthenticated() {
        Authentication authentication = getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * 检查当前用户是否具有指定角色
     */
    public boolean hasRole(String role) {
        Authentication authentication = getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    /**
     * 检查当前用户是否具有指定权限
     */
    public boolean hasAuthority(String authority) {
        Authentication authentication = getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(authority));
    }

    /**
     * 获取当前请求的客户端IP地址
     */
    public String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        return getClientIpFromRequest(request);
    }

    /**
     * 获取当前请求的用户代理
     */
    public String getUserAgent() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        return request.getHeader("User-Agent");
    }

    /**
     * 获取当前请求的请求URI
     */
    public String getRequestUri() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        return request.getRequestURI();
    }

    /**
     * 获取当前请求的方法
     */
    public String getRequestMethod() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        return request.getMethod();
    }

    /**
     * 记录安全审计日志
     */
    public void logSecurityAudit(String action, String details) {
        String username = getCurrentUsername();
        String ip = getClientIp();
        String userAgent = getUserAgent();
        String requestUri = getRequestUri();
        String requestMethod = getRequestMethod();

        log.info("安全审计 - 用户: {}, IP: {}, 动作: {}, 详情: {}, URI: {}, 方法: {}, UserAgent: {}",
                username, ip, action, details, requestUri, requestMethod, userAgent);
    }

    /**
     * 从HttpServletRequest中提取客户端真实IP
     */
    private String getClientIpFromRequest(HttpServletRequest request) {
        String ip = null;

        // 尝试从X-Forwarded-For获取（经过代理的情况）
        ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip)) {
            // 可能包含多个IP，取第一个
            int index = ip.indexOf(",");
            if (index != -1) {
                ip = ip.substring(0, index);
            }
            return ip.trim();
        }

        // 尝试从Proxy-Client-IP获取
        ip = request.getHeader("Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }

        // 尝试从WL-Proxy-Client-IP获取
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }

        // 尝试从HTTP_CLIENT_IP获取
        ip = request.getHeader("HTTP_CLIENT_IP");
        if (isValidIp(ip)) {
            return ip.trim();
        }

        // 尝试从HTTP_X_FORWARDED_FOR获取
        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (isValidIp(ip)) {
            return ip.trim();
        }

        // 最后使用RemoteAddr
        ip = request.getRemoteAddr();
        if (isValidIp(ip)) {
            return ip.trim();
        }

        return "unknown";
    }

    /**
     * 验证IP地址是否有效
     */
    private boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

    /**
     * 获取Authentication对象
     */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 自定义用户详情类（如果使用）
     */
    public static class CustomUserDetails extends org.springframework.security.core.userdetails.User {
        private final Long userId;
        private final String email;
        private final String phone;

        public CustomUserDetails(String username, String password,
                                 boolean enabled, boolean accountNonExpired,
                                 boolean credentialsNonExpired, boolean accountNonLocked,
                                 List<GrantedAuthority> authorities,
                                 Long userId, String email, String phone) {
            super(username, password, enabled, accountNonExpired,
                    credentialsNonExpired, accountNonLocked, authorities);
            this.userId = userId;
            this.email = email;
            this.phone = phone;
        }

        public Long getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }
    }
}