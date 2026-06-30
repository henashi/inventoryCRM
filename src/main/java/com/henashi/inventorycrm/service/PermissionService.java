package com.henashi.inventorycrm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.henashi.inventorycrm.dto.UserPermissionDTO;
import com.henashi.inventorycrm.pojo.Permission;
import com.henashi.inventorycrm.pojo.RolePermission;
import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.pojo.UserPermission;
import com.henashi.inventorycrm.repository.PermissionRepository;
import com.henashi.inventorycrm.repository.RolePermissionRepository;
import com.henashi.inventorycrm.repository.UserPermissionRepository;
import com.henashi.inventorycrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionService {

    private final UserPermissionRepository userPermissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 判断用户是否有指定权限
     * 判定链路: user_permission → role_permission → permission.defaultRoles 默认值
     */
    public boolean hasPermission(Long userId, String permissionKey) {
        // 1. 用户级覆盖
        Optional<UserPermission> userPerm = userPermissionRepository
                .findByUserIdAndPermission_Key(userId, permissionKey);
        if (userPerm.isPresent()) {
            return userPerm.get().getEnabled();
        }

        // 2. 角色级覆盖
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;

        String roleName = userOpt.get().getRole() != null
                ? userOpt.get().getRole().getName()
                : null;
        if (roleName == null) return false;

        Optional<RolePermission> rolePerm = rolePermissionRepository
                .findByRole_NameAndPermission_Key(roleName, permissionKey);
        if (rolePerm.isPresent()) {
            return rolePerm.get().getEnabled();
        }

        // 3. permission.defaultRoles 默认值
        Optional<Permission> permOpt = permissionRepository.findByKey(permissionKey);
        if (permOpt.isEmpty()) return false;

        String defaultRoles = permOpt.get().getDefaultRoles();
        if (defaultRoles == null || defaultRoles.isBlank()) return false;

        try {
            List<String> roles = objectMapper.readValue(defaultRoles, new TypeReference<>() {});
            return roles.stream().anyMatch(r -> r.equalsIgnoreCase(roleName));
        } catch (JsonProcessingException e) {
            log.warn("权限 [{}] 的默认角色 JSON 解析失败: {}", permissionKey, defaultRoles, e);
            return false;
        }
    }

    /**
     * 获取用户所有权限（逐条走三层判定链）
     */
    public List<UserPermissionDTO> getUserPermissions(Long userId) {
        List<Permission> allPerms = permissionRepository.findAll();
        return allPerms.stream()
                .map(p -> new UserPermissionDTO(
                        p.getKey(),
                        p.getName(),
                        p.getModule(),
                        p.getModuleName(),
                        p.getType(),
                        hasPermission(userId, p.getKey())
                ))
                .toList();
    }
}
