package com.henashi.inventorycrm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记在不需要记录操作日志的 Controller 方法上。
 * <p>
 * 适用于：分页查询、搜索、详情查看等只读操作，
 * 或某些不应进入操作日志的内部端点。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLogIgnore {
}
