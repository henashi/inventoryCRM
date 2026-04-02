package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record OperationLogCreateDTO(
        // 操作信息
        @NotBlank(message = "模块名称不能为空")
        @Size(max = 50, message = "模块名称不能超过50字符")
        @Schema(description = "模块名称")
        String module,

        @NotBlank(message = "操作类型不能为空")
        @Size(max = 50, message = "操作类型不能超过50字符")
        @Schema(description = "操作类型")
        String operationType,

        @NotBlank(message = "操作描述不能为空")
        @Size(max = 200, message = "操作描述不能超过200字符")
        @Schema(description = "操作描述")
        String description,

        // 请求信息
        @Size(max = 500, message = "请求URL不能超过500字符")
        @Schema(description = "请求URL")
        String requestUrl,

        @Size(max = 10, message = "请求方法不能超过10字符")
        @Schema(description = "请求方法")
        String requestMethod,

        // 操作人信息
        @Size(max = 50, message = "操作人不能超过50字符")
        @Schema(description = "操作人")
        String operator,

        @Size(max = 50, message = "IP地址不能超过50字符")
        @Schema(description = "IP地址")
        String ipAddress,

        // 结果信息
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态必须为0或1")
        @Max(value = 1, message = "状态必须为0或1")
        @Schema(description = "操作状态，0-失败, 1-成功")
        Integer status,  // 0-失败, 1-成功

        @Size(max = 1000, message = "错误信息不能超过1000字符")
        @Schema(description = "错误信息")
        String errorMessage,

        @PositiveOrZero(message = "执行时间不能为负数")
        @Schema(description = "执行时间，单位毫秒")
        Long executionTime
) {
}
