package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.annotation.RequirePermission;
import static com.henashi.inventorycrm.constants.Permissions.*;
import com.henashi.inventorycrm.pojo.Permission;
import com.henashi.inventorycrm.service.PermissionDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/permission-defs")
@Tag(name = "权限定义管理", description = "权限点 CRUD（替代 DataDict PERMISSION 分组）")
public class PermissionDefinitionController {

    private final PermissionDefinitionService service;

    @GetMapping
    @Operation(summary = "获取所有权限定义")
    public List<Permission> listAll() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据 ID 查询权限定义")
    public Permission getById(@PathVariable @NotNull @Min(1) Long id) {
        return service.findById(id);
    }

    @PostMapping
    @RequirePermission(DATA_DICTS_MANAGE)
    @Operation(summary = "新增权限定义")
    public ResponseEntity<Permission> create(
            @RequestParam String key,
            @RequestParam String name,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String moduleName,
            @RequestParam(defaultValue = "ACTION") String type,
            @RequestParam(required = false) String defaultRoles,
            @RequestParam(required = false) String description) {
        Permission perm = service.create(key, name, module, moduleName, type, defaultRoles, description);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(perm.getId()).toUri();
        return ResponseEntity.created(location).body(perm);
    }

    @PutMapping("/{id}")
    @RequirePermission(DATA_DICTS_MANAGE)
    @Operation(summary = "更新权限定义")
    public Permission update(
            @PathVariable @NotNull @Min(1) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String defaultRoles,
            @RequestParam(required = false) String description) {
        return service.update(id, name, moduleName, type, defaultRoles, description);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(DATA_DICTS_MANAGE)
    @Operation(summary = "删除权限定义")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Min(1) Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
