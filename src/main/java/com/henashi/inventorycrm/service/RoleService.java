package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.pojo.Role;
import com.henashi.inventorycrm.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<Role> listAll() {
        return roleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Role findById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND", "角色不存在: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    @Transactional
    public Role create(String name, String displayName, String description, Integer sortOrder) {
        if (roleRepository.existsByName(name)) {
            throw new BusinessException("ROLE_EXISTS", "角色已存在: " + name);
        }
        Role role = Role.builder()
                .name(name.toUpperCase())
                .displayName(displayName)
                .description(description)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .status("1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return roleRepository.save(role);
    }

    @Transactional
    public Role update(Long id, String displayName, String description, Integer sortOrder, String status) {
        Role role = findById(id);

        if (displayName != null) {
            role.setDisplayName(displayName);
        }
        if (description != null) {
            role.setDescription(description);
        }
        if (sortOrder != null) {
            role.setSortOrder(sortOrder);
        }
        if (status != null) {
            role.setStatus(status);
        }
        role.setUpdatedAt(LocalDateTime.now());

        return roleRepository.save(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = findById(id);
        // 防止删除系统内置角色
        String name = role.getName();
        if ("ADMIN".equals(name) || "MANAGER".equals(name) || "USER".equals(name)) {
            throw new BusinessException("ROLE_PROTECTED", "系统内置角色 [" + name + "] 不可删除");
        }
        roleRepository.delete(role);
        log.info("角色已删除: {} ({})", role.getName(), role.getDisplayName());
    }
}
