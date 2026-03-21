package com.henashi.inventorycrm.dto;

import java.time.LocalDateTime;

public record UserDTO(
        Long id,
        String username,
        String role,
        Integer status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdTime,
        String remark) {
}