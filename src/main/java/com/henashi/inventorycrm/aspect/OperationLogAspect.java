package com.henashi.inventorycrm.aspect;

import com.henashi.inventorycrm.annotation.OperationLogIgnore;
import com.henashi.inventorycrm.dto.OperationLogCreateDTO;
import com.henashi.inventorycrm.enums.OperationType;
import com.henashi.inventorycrm.service.OperationLogService;
import com.henashi.inventorycrm.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Controller 层全局操作日志切面。
 * <p>
 * 拦截所有 Controller 方法（不含 @OperationLogIgnore），
 * 自动记录操作人、操作类型、模块、IP、执行耗时等信息到 operation_log 表。
 * GET 请求默认跳过（只读操作不记日志）。
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    /** URL 前缀 → 中文模块名映射 */
    private static final Map<String, String> MODULE_MAP = Map.ofEntries(
            Map.entry("/api/auth", "登录认证"),
            Map.entry("/api/customers", "客户管理"),
            Map.entry("/api/orders", "订单管理"),
            Map.entry("/api/data-dict", "数据字典"),
            Map.entry("/api/gifts", "礼品管理"),
            Map.entry("/api/gift-logs", "礼品管理"),
            Map.entry("/api/inventories", "库存管理"),
            Map.entry("/api/inventory-logs", "库存管理"),
            Map.entry("/api/operation-logs", "系统管理"),
            Map.entry("/api/products", "商品管理"),
            Map.entry("/api/system-configs", "系统管理"),
            Map.entry("/api/users", "用户管理"),
            Map.entry("/api/ai", "AI 助手")
    );

    /** HTTP 方法 → OperationType 映射 */
    private static final Map<String, OperationType> METHOD_TYPE_MAP = Map.of(
            "POST", OperationType.CREATE,
            "PUT", OperationType.CONTENT_UPDATE,
            "PATCH", OperationType.STATUS_UPDATE,
            "DELETE", OperationType.DELETE
    );

    /** 操作动词映射（用于生成描述） */
    private static final Map<String, String> METHOD_ACTION_MAP = Map.of(
            "POST", "创建",
            "PUT", "修改",
            "PATCH", "修改",
            "DELETE", "删除"
    );

    @Around("execution(* com.henashi.inventorycrm.controller..*(..)) " +
            "|| execution(* com.henashi.inventorycrm.ai.controller..*(..))")
    public Object logOperation(ProceedingJoinPoint jp) throws Throwable {
        // --- 前置检查 ---

        // 1) 跳过 GET 请求（只读）
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return jp.proceed();
        }
        String httpMethod = request.getMethod();
        if ("GET".equalsIgnoreCase(httpMethod)) {
            return jp.proceed();
        }

        // 2) 跳过 @OperationLogIgnore 标记的方法
        MethodSignature signature = (MethodSignature) jp.getSignature();
        if (signature.getMethod().isAnnotationPresent(OperationLogIgnore.class)) {
            return jp.proceed();
        }

        // 3) 跳过非 Api 路径（如 error/error 等框架端点）
        String requestUri = request.getRequestURI();
        if (!requestUri.startsWith("/api/")) {
            return jp.proceed();
        }

        // --- 提取日志所需信息 ---
        String module = resolveModule(requestUri);
        OperationType operationType = resolveOperationType(httpMethod, jp);
        String actionVerb = METHOD_ACTION_MAP.getOrDefault(httpMethod, httpMethod);
        String description = buildDescription(module, actionVerb, jp);

        String operator = SecurityUtils.getCurrentUsername();
        String ipAddress = SecurityUtils.getClientIp();
        long startTime = System.currentTimeMillis();

        // --- 执行目标方法 ---
        Object result;
        boolean success = true;
        String errorMsg = null;
        try {
            result = jp.proceed();
            // 检查响应状态码
            if (result instanceof ResponseEntity<?> response) {
                int statusCode = response.getStatusCode().value();
                if (statusCode < 200 || statusCode >= 400) {
                    success = false;
                    errorMsg = "HTTP " + statusCode;
                }
            }
        } catch (Throwable e) {
            success = false;
            errorMsg = e.getMessage();
            throw e; // 继续向外抛，不影响业务
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // fire-and-forget 写入操作日志（失败不影响业务）
            try {
                OperationLogCreateDTO logDTO = new OperationLogCreateDTO(
                        module,
                        operationType,
                        description,
                        requestUri,
                        httpMethod,
                        operator,
                        ipAddress,
                        success ? 1 : 0,
                        errorMsg,
                        executionTime
                );
                operationLogService.logOperation(logDTO);
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
        }

        return result;
    }

    // ========== 私有辅助方法 ==========

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /** 从 URL 前缀匹配模块名 */
    private String resolveModule(String uri) {
        return MODULE_MAP.entrySet().stream()
                .filter(e -> uri.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("系统管理");
    }

    /** 从 HTTP 方法 + 方法名推断操作类型 */
    private OperationType resolveOperationType(String httpMethod, ProceedingJoinPoint jp) {
        // DELETE → DELETE
        if ("DELETE".equalsIgnoreCase(httpMethod)) {
            return OperationType.DELETE;
        }

        // PATCH → STATUS_UPDATE
        if ("PATCH".equalsIgnoreCase(httpMethod)) {
            return OperationType.STATUS_UPDATE;
        }

        // POST/ PUT → 根据方法名细分
        String methodName = jp.getSignature().getName().toLowerCase();
        if (methodName.contains("status")) {
            return OperationType.STATUS_UPDATE;
        }
        if ("POST".equalsIgnoreCase(httpMethod)) {
            return OperationType.CREATE;
        }
        // PUT
        if (methodName.contains("content") || methodName.contains("info")) {
            return OperationType.CONTENT_UPDATE;
        }
        return OperationType.BOTH_UPDATE;
    }

    /** 生成人类可读的操作描述 */
    private String buildDescription(String module, String actionVerb, ProceedingJoinPoint jp) {
        String methodName = jp.getSignature().getName();
        // 尝试提取 ID 值（第一个路径变量或 Long 类型参数）
        String idInfo = extractIdFromArgs(jp);
        if (idInfo != null) {
            return module + " - " + actionVerb + " #" + idInfo;
        }
        return module + " - " + actionVerb;
    }

    /** 从参数中提取 ID 用于描述 */
    private String extractIdFromArgs(ProceedingJoinPoint jp) {
        Object[] args = jp.getArgs();
        if (args == null) return null;

        MethodSignature sig = (MethodSignature) jp.getSignature();
        String[] paramNames = sig.getParameterNames();
        if (paramNames == null) return null;

        for (int i = 0; i < Math.min(args.length, paramNames.length); i++) {
            String name = paramNames[i];
            Object val = args[i];
            if (val != null && (name.equals("id") || name.endsWith("Id"))
                    && (val instanceof Long || val instanceof Integer)) {
                return String.valueOf(val);
            }
        }
        return null;
    }
}
