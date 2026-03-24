package com.henashi.inventorycrm.annotation;

import com.henashi.inventorycrm.pojo.InventoryLog.LogType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 库存操作日志注解
 * 用于标记需要进行库存日志记录的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InventoryAudit {

    /**
     * 操作类型
     * in：入库
     * out：出库
     * adjust：库存调整
     * create：创建商品
     * param
     */
    LogType operationType();

    /**
     * 操作类型参数
     */
    String operationTypeParam() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 商品ID参数名（默认为第一个参数）
     */
    String productIdParam() default "";

    /**
     * 数量参数名
     */
    String quantityParam() default "";

    /**
     * 原因参数名
     */
    String reasonParam() default "";

    /**
     * 操作人参数名
     */
    String operatorParam() default "";

    /**
     * 是否记录操作前后的库存
     */
    boolean recordStockChange() default true;

    /**
     * 是否在操作失败时记录日志
     */
    boolean logOnFailure() default true;
}

