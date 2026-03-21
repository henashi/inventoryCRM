package com.henashi.inventorycrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateDTO(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度3-50字符")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 20, message = "密码长度6-20字符")
        String password,

        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "ADMIN|USER|MANAGER", message = "角色必须为ADMIN/USER/MANAGER")
        String role,

        @Size(max = 200, message = "备注不能超过200字符")
        String remark
) {
}
