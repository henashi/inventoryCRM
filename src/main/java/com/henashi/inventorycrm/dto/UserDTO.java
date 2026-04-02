package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record UserDTO(
        @Schema(description = "主键ID")
        Long id,

        @Schema(description = "用户名")
        String username,

        @Schema(description = "角色")
        String role,

        @Schema(description = "状态：0-禁用，1-启用")
        Integer status,

        @Schema(description = "最后登录时间")
        LocalDateTime lastLoginAt,

        @Schema(description = "创建时间")
        LocalDateTime createdTime,

        @Schema(description = "备注")
        String remark) {
}