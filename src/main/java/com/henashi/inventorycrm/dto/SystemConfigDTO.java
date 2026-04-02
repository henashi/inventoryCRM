package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record SystemConfigDTO(
        @Schema(description = "主键ID")
        Long id,

        @Schema(description = "配置键")
        String configKey,

        @Schema(description = "配置值")
        String configValue,

        @Schema(description = "描述")
        String description,

        @Schema(description = "配置分组")
        String configGroup,

        @Schema(description = "创建时间")
        LocalDateTime createdTime,

        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
