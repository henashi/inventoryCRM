package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.pojo.Permission;
import com.henashi.inventorycrm.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionDefinitionService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<Permission> listAll() {
        return permissionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Permission findById(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("PERM_NOT_FOUND", "权限不存在: " + id));
    }

    @Transactional
    public Permission create(String key, String name, String module, String moduleName,
                             String type, String defaultRoles, String description) {
        if (permissionRepository.existsByKey(key)) {
            throw new BusinessException("PERM_EXISTS", "权限标识已存在: " + key);
        }
        Permission perm = Permission.builder()
                .key(key)
                .name(name)
                .module(module)
                .moduleName(moduleName)
                .type(type != null ? type : "ACTION")
                .defaultRoles(defaultRoles)
                .description(description)
                .sortOrder(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return permissionRepository.save(perm);
    }

    @Transactional
    public Permission update(Long id, String name, String moduleName, String type,
                             String defaultRoles, String description) {
        Permission perm = findById(id);
        if (name != null) perm.setName(name);
        if (moduleName != null) perm.setModuleName(moduleName);
        if (type != null) perm.setType(type);
        if (defaultRoles != null) perm.setDefaultRoles(defaultRoles);
        if (description != null) perm.setDescription(description);
        perm.setUpdatedAt(LocalDateTime.now());
        return permissionRepository.save(perm);
    }

    @Transactional
    public void delete(Long id) {
        Permission perm = findById(id);
        permissionRepository.delete(perm);
        log.info("权限已删除: {} ({})", perm.getKey(), perm.getName());
    }
}
