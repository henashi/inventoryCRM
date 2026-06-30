package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.pojo.Permission;
import com.henashi.inventorycrm.pojo.Role;
import com.henashi.inventorycrm.pojo.RolePermission;
import com.henashi.inventorycrm.repository.PermissionRepository;
import com.henashi.inventorycrm.repository.RolePermissionRepository;
import com.henashi.inventorycrm.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository repository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public List<RolePermission> getPermissionsByRole(String roleName) {
        return repository.findByRole_Name(roleName);
    }

    public Map<String, Boolean> getPermissionMapByRole(String roleName) {
        return repository.findByRole_Name(roleName).stream()
                .collect(Collectors.toMap(
                        rp -> rp.getPermission().getKey(),
                        RolePermission::getEnabled
                ));
    }

    @Transactional
    public void updatePermissions(String roleName, Map<String, Boolean> permissions) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "角色不存在: " + roleName));

        repository.deleteByRole_Name(roleName);

        List<RolePermission> entities = permissions.entrySet().stream()
                .map(entry -> {
                    Permission permission = permissionRepository.findByKey(entry.getKey())
                            .orElseThrow(() -> new BusinessException("PERM_NOT_FOUND",
                                    "权限不存在: " + entry.getKey()));
                    return RolePermission.builder()
                            .role(role)
                            .permission(permission)
                            .enabled(entry.getValue())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                })
                .toList();

        repository.saveAll(entities);
    }

    public List<String> getEnabledPermissionKeys(String roleName) {
        return repository.findByRole_NameAndEnabled(roleName, true).stream()
                .map(rp -> rp.getPermission().getKey())
                .toList();
    }
}
