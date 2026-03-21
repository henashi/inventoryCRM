package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.SystemConfig;

import java.time.LocalDateTime;

public record SystemConfigDTO(
        Long id,
        String configKey,
        String configValue,
        String description,
        String configGroup,
        LocalDateTime createdTime,
        LocalDateTime updatedAt
) {
    public static SystemConfigDTO fromEntity(SystemConfig entity) {
        return new SystemConfigDTO(
                entity.getId(),
                entity.getConfigKey(),
                entity.getConfigValue(),
                entity.getDescription(),
                entity.getConfigGroup(),
                entity.getCreatedTime(),
                entity.getUpdatedTime()
        );
    }

    public SystemConfig toEntity() {
        return SystemConfig.builder()
                .configKey(this.configKey)
                .configValue(this.configValue)
                .description(this.description)
                .configGroup(this.configGroup != null ? this.configGroup : "default")
                .build();
    }
}
