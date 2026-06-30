package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.pojo.Permission;
import com.henashi.inventorycrm.pojo.UserPermission;
import com.henashi.inventorycrm.repository.PermissionRepository;
import com.henashi.inventorycrm.repository.UserPermissionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users/{userId}/permissions")
@Tag(name = "用户权限", description = "用户级别权限覆盖接口")
public class UserPermissionController {

    private final UserPermissionRepository repository;
    private final PermissionRepository permissionRepository;

    @GetMapping
    @Operation(summary = "查询指定用户的权限覆盖")
    public ResponseEntity<Map<String, Boolean>> getUserPermissions(@PathVariable("userId") Long userId) {
        List<UserPermission> perms = repository.findByUserId(userId);
        Map<String, Boolean> map = perms.stream()
                .collect(Collectors.toMap(
                        up -> up.getPermission().getKey(),
                        UserPermission::getEnabled
                ));
        return ResponseEntity.ok(map);
    }

    @PutMapping
    @Operation(summary = "更新指定用户的权限覆盖")
    @Transactional
    public ResponseEntity<Void> updateUserPermissions(
            @PathVariable("userId") Long userId,
            @RequestBody Map<String, Boolean> permissions) {
        repository.deleteByUserId(userId);

        List<UserPermission> entities = permissions.entrySet().stream()
                .map(entry -> {
                    Permission permission = permissionRepository.findByKey(entry.getKey())
                            .orElseThrow(() -> new BusinessException("PERM_NOT_FOUND",
                                    "权限不存在: " + entry.getKey()));
                    return UserPermission.builder()
                            .userId(userId)
                            .permission(permission)
                            .enabled(entry.getValue())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                })
                .toList();

        repository.saveAll(entities);
        return ResponseEntity.ok().build();
    }
}
