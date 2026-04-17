package com.henashi.inventorycrm.pojo;


import com.henashi.inventorycrm.enums.OperationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "operation_log")
@SQLRestriction(value = "deleted = false")
@SQLDelete(sql = "update operation_log set deleted = true where id = ?")
public class OperationLog extends BaseEntity {

    /**
     * 操作模块
     */
    @Column(name = "module", length = 50)
    private String module;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", length = 50)
    private OperationType operationType;

    /**
     * 操作描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 请求URL
     */
    @Column(name = "request_url", length = 500)
    private String requestUrl;

    /**
     * 请求方法
     */
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    /**
     * 请求参数
     */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    /**
     * 操作人
     */
    @Column(name = "operator", length = 50)
    private String operator;

    /**
     * 操作IP
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /*
     * 操作状态：1-成功 0-失败
     */
//    @Column(name = "status")
//    private Integer status = 1;

    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 执行时间（毫秒）
     */
    @Column(name = "execution_time")
    private Long executionTime;

    /**
     * 操作时间
     */
    @CreationTimestamp
    @Column(name = "operation_time", updatable = false)
    private LocalDateTime operationTime;

}