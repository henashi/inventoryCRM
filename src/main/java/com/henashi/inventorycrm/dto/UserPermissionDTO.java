package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "用户权限条目（含判定结果）")
public record UserPermissionDTO(
        @Schema(description = "权限标识") String key,
        @Schema(description = "权限名称") String name,
        @Schema(description = "所属模块标识") String module,
        @Schema(description = "模块中文名") String moduleName,
        @Schema(description = "类型：MENU/API/ACTION") String type,
        @Schema(description = "当前用户是否拥有此权限") boolean enabled
) {}
