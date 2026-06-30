package com.henashi.inventorycrm.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.henashi.inventorycrm.pojo.Permission;
import com.henashi.inventorycrm.repository.PermissionRepository;
import com.henashi.inventorycrm.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/admin/permissions")
@Tag(name = "角色权限", description = "角色权限配置接口")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;
    private final PermissionRepository permissionRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/{role}")
    @Operation(summary = "查询指定角色的权限映射（合并 permission 默认值 + 角色覆盖）")
    public ResponseEntity<Map<String, Boolean>> getPermissions(@PathVariable("role") String role) {
        List<Permission> allPerms = permissionRepository.findAll();
        Map<String, Boolean> roleOverrides = rolePermissionService.getPermissionMapByRole(role);

        Map<String, Boolean> result = new HashMap<>();
        for (Permission p : allPerms) {
            String key = p.getKey();
            if (roleOverrides.containsKey(key)) {
                result.put(key, roleOverrides.get(key));
            } else {
                result.put(key, roleInDefaultValue(p.getDefaultRoles(), role));
            }
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/list-definitions")
    @Operation(summary = "获取所有权限定义（从 sys_permission 表）")
    public ResponseEntity<List<Map<String, String>>> getPermissionDefinitions() {
        List<Permission> perms = permissionRepository.findAll();
        List<Map<String, String>> result = perms.stream()
                .map(p -> {
                    java.util.HashMap<String, String> m = new java.util.HashMap<>();
                    m.put("key", p.getKey());
                    m.put("name", p.getName());
                    m.put("module", p.getModule() != null ? p.getModule() : "");
                    m.put("moduleName", p.getModuleName() != null ? p.getModuleName() : p.getModule());
                    m.put("type", p.getType() != null ? p.getType() : "ACTION");
                    m.put("defaultRoles", p.getDefaultRoles() != null ? p.getDefaultRoles() : "[]");
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{role}")
    @Operation(summary = "更新指定角色的权限映射")
    public ResponseEntity<Void> updatePermissions(
            @PathVariable("role") String role,
            @RequestBody Map<String, Boolean> permissions) {
        rolePermissionService.updatePermissions(role, permissions);
        return ResponseEntity.ok().build();
    }

    /**
     * 解析 JSON 数组格式的默认角色值，判断指定角色是否在其中
     */
    private boolean roleInDefaultValue(String defaultValue, String role) {
        if (defaultValue == null || defaultValue.isBlank()) return false;
        try {
            List<String> roles = objectMapper.readValue(defaultValue, new TypeReference<>() {});
            return roles.stream().anyMatch(r -> r.equalsIgnoreCase(role));
        } catch (JsonProcessingException e) {
            log.warn("权限默认值 JSON 解析失败: {}", defaultValue, e);
            return false;
        }
    }
}
