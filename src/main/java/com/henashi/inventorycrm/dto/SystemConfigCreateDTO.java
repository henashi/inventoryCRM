package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record SystemConfigCreateDTO(
        // 配置信息
        @Schema(description = "配置键，例：app.name")
        @NotBlank(message = "配置键不能为空")
        @Size(max = 100, message = "配置键不能超过100字符")
        @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "配置键只能包含字母、数字、点和下划线")
        String configKey,

        @Schema(description = "配置值")
        @NotBlank(message = "配置值不能为空")
        @Size(max = 1000, message = "配置值不能超过1000字符")
        String configValue,

        // 描述和分组
        @Schema(description = "描述")
        @NotBlank(message = "描述不能为空")
        @Size(max = 200, message = "描述不能超过200字符")
        String description,

        @Schema(description = "配置分组")
        @NotBlank(message = "配置分组不能为空")
        @Size(max = 50, message = "配置分组不能超过50字符")
        String configGroup
) {
}