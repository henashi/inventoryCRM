package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.annotation.RequirePermission;
import static com.henashi.inventorycrm.constants.Permissions.*;
import com.henashi.inventorycrm.pojo.Role;
import com.henashi.inventorycrm.service.RoleService;
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
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/roles")
@Tag(name = "角色管理", description = "角色 CRUD 接口（支持动态角色）")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "获取所有角色列表")
    public List<Role> listAll() {
        return roleService.listAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据 ID 查询角色")
    public Role getById(@PathVariable @NotNull @Min(1) Long id) {
        return roleService.findById(id);
    }

    @PostMapping
    @RequirePermission(USERS_MANAGE)
    @Operation(summary = "创建新角色")
    public ResponseEntity<Role> create(@RequestParam String name,
                                       @RequestParam(required = false) String displayName,
                                       @RequestParam(required = false) String description,
                                       @RequestParam(required = false) Integer sortOrder) {
        Role role = roleService.create(name, displayName, description, sortOrder);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(role.getId()).toUri();
        return ResponseEntity.created(location).body(role);
    }

    @PutMapping("/{id}")
    @RequirePermission(USERS_MANAGE)
    @Operation(summary = "更新角色信息")
    public Role update(@PathVariable @NotNull @Min(1) Long id,
                       @RequestParam(required = false) String displayName,
                       @RequestParam(required = false) String description,
                       @RequestParam(required = false) Integer sortOrder,
                       @RequestParam(required = false) String status) {
        return roleService.update(id, displayName, description, sortOrder, status);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(USERS_MANAGE)
    @Operation(summary = "删除角色（内置角色不可删除）")
    public ResponseEntity<Void> delete(@PathVariable @NotNull @Min(1) Long id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
