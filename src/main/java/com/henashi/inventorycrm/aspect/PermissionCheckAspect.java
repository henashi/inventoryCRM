package com.henashi.inventorycrm.aspect;

import com.henashi.inventorycrm.annotation.RequirePermission;
import com.henashi.inventorycrm.security.CustomUserDetails;
import com.henashi.inventorycrm.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 细粒度权限校验切面
 * 拦截 @RequirePermission 注解，在 Controller 方法执行前校验当前用户是否具有指定权限
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionCheckAspect {

    private final PermissionService permissionService;

    @Before("@annotation(com.henashi.inventorycrm.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        if (annotation == null) {
            return;
        }

        String permissionKey = annotation.value();

        // 从 SecurityContext 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("未认证用户尝试访问需要权限的方法: {}", permissionKey);
            throw new AccessDeniedException("请先登录");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            log.warn("无法获取用户身份信息");
            throw new AccessDeniedException("无法验证用户身份");
        }

        Long userId = userDetails.getId();
        boolean hasPermission = permissionService.hasPermission(userId, permissionKey);

        if (!hasPermission) {
            log.warn("用户 {} 权限不足: 需要 [{}]", userDetails.getUsername(), permissionKey);
            throw new AccessDeniedException("权限不足，需要 [" + permissionKey + "] 权限");
        }

        log.debug("用户 {} 通过权限校验: [{}]", userDetails.getUsername(), permissionKey);
    }
}
