package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record OperationLogDTO(
        @Schema(description = "日志ID")
        Long id,
        @Schema(description = "模块")
        String module,
        @Schema(description = "操作类型")
        String operationType,
        @Schema(description = "操作描述")
        String description,
        @Schema(description = "请求URL")
        String requestUrl,
        @Schema(description = "请求方法")
        String requestMethod,
        @Schema(description = "操作人")
        String operator,
        @Schema(description = "IP地址")
        String ipAddress,
        @Schema(description = "操作状态，0-失败, 1-成功")
        Integer status,
        @Schema(description = "错误信息")
        String errorMessage,
        @Schema(description = "执行时间，单位毫秒")
        Long executionTime,
        @Schema(description = "创建时间")
        LocalDateTime operationTime
) {
}

