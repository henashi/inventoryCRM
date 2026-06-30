package com.henashi.inventorycrm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 细粒度权限校验注解
 * 标注在 Controller 方法上，指定需要的权限 key
 * 与 Spring Security 的 hasRole 形成 AND 关系：先过角色粗粒度，再过权限细粒度
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 权限标识，如 "customers:delete"
     */
    String value();
}
