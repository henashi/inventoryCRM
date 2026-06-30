package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.UserPermissionDTO;
import com.henashi.inventorycrm.security.CustomUserDetails;
import com.henashi.inventorycrm.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "用户权限", description = "当前用户权限查询接口")
public class MyPermissionController {

    private final PermissionService permissionService;

    @GetMapping("/permissions")
    @Operation(summary = "获取当前用户的全部权限", description = "逐条走三层判定链，返回含 type/enabled 的完整权限列表")
    public List<UserPermissionDTO> getMyPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return List.of();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return permissionService.getUserPermissions(userDetails.getUser().getId());
        }
        return List.of();
    }
}
