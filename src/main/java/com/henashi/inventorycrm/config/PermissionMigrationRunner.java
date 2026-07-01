package com.henashi.inventorycrm.config;

import com.henashi.inventorycrm.pojo.DataDict;
import com.henashi.inventorycrm.pojo.Permission;
import com.henashi.inventorycrm.pojo.Role;
import com.henashi.inventorycrm.repository.DataDictRepository;
import com.henashi.inventorycrm.repository.PermissionRepository;
import com.henashi.inventorycrm.repository.RoleRepository;
import com.henashi.inventorycrm.repository.UserRepository;
import com.henashi.inventorycrm.service.UserService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据迁移：将旧版字符串角色/权限模式迁移到新版实体模式
 * 在 initPermissionDefaults 和 createDefaultUsers 之后执行
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class PermissionMigrationRunner implements CommandLineRunner {

    private static final Map<String, String> MODULE_LABELS = Map.ofEntries(
            Map.entry("dashboard", "仪表盘"),
            Map.entry("customers", "客户管理"),
            Map.entry("products", "商品管理"),
            Map.entry("inventory", "库存管理"),
            Map.entry("orders", "订单管理"),
            Map.entry("gifts", "礼品管理"),
            Map.entry("giftLogs", "礼品发放日志"),
            Map.entry("dataDicts", "配置管理"),
            Map.entry("users", "用户管理"),
            Map.entry("operationLogs", "系统日志"),
            Map.entry("ai", "AI 功能")
    );

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final DataDictRepository dataDictRepository;
    private final EntityManager entityManager;
    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        try {
            migrateRoles();
            migratePermissions();
            migrateUserRoles();
            migrateRolePermissions();
            migrateUserPermissions();
            createDefaultUsers();
            log.info("权限/角色数据迁移完成");
        } catch (Exception e) {
            log.error("权限/角色迁移异常，启动将继续但部分功能可能受限", e);
        }
    }

    private void migrateRoles() {
        if (roleRepository.count() > 0) {
            log.info("角色表已有数据，跳过角色迁移");
            return;
        }

        Role[] defaultRoles = {
                createRole("ADMIN", "管理员", "系统管理员，拥有所有权限", 1),
                createRole("MANAGER", "经理", "业务经理，可管理大部分业务操作", 2),
                createRole("USER", "普通用户", "普通用户，仅有查看和基本操作权限", 3)
        };

        for (Role role : defaultRoles) {
            roleRepository.save(role);
            log.info("创建默认角色: {} ({})", role.getName(), role.getDisplayName());
        }
    }

    private Role createRole(String name, String displayName, String description, int sortOrder) {
        return Role.builder()
                .name(name)
                .displayName(displayName)
                .description(description)
                .sortOrder(sortOrder)
                .status("1")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void migratePermissions() {
        if (permissionRepository.count() > 0) {
            // 已有数据但 moduleName 可能为空（升级场景），补填
            fillModuleNames();
            return;
        }

        List<DataDict> dicts = dataDictRepository.findByGroupCode("PERMISSION");
        if (dicts.isEmpty()) {
            log.info("data_dict 中无 PERMISSION 数据，跳过权限迁移");
            return;
        }

        for (DataDict dict : dicts) {
            String key = dict.getParamCode();
            String name = dict.getParamName();
            String defaultRoles = dict.getParamValue();
            String module = key.contains(":") ? key.split(":")[0] : "other";
            String moduleName = MODULE_LABELS.getOrDefault(module, module);

            // 根据 key 推断 type
            String permType = "ACTION";
            if (key.contains(":")) {
                String action = key.split(":")[1];
                if ("view".equals(action)) {
                    permType = "MENU";
                } else if ("create".equals(action) || "edit".equals(action) || "delete".equals(action)
                        || "import".equals(action) || "export".equals(action) || "enable".equals(action)
                        || "manage".equals(action)) {
                    permType = "ACTION";
                } else {
                    permType = "API";
                }
            }

            Permission permission = Permission.builder()
                    .key(key)
                    .name(name)
                    .module(module)
                    .moduleName(moduleName)
                    .type(permType)
                    .defaultRoles(defaultRoles)
                    .sortOrder(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            permissionRepository.save(permission);
            log.debug("迁移权限: {} ({})", key, name);
        }
        log.info("已迁移 {} 条权限定义", dicts.size());
    }

    private void createDefaultUsers() {
        if (userRepository.findByUsername("admin").isPresent()) {
            log.info("默认用户已存在，跳过创建");
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ADMIN").displayName("管理员")
                        .description("系统管理员").sortOrder(0).status("1")
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()));
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").displayName("普通用户")
                        .description("普通用户").sortOrder(0).status("1")
                        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()));

        createUser("admin", "admin123", adminRole);
        createUser("user", "user123", userRole);
        createUser("demo", "demo123", userRole);
        log.info("默认用户创建完成: admin/admin123, user/user123, demo/demo123");
    }

    private void createUser(String username, String password, Role role) {
        try {
            com.henashi.inventorycrm.pojo.User user = new com.henashi.inventorycrm.pojo.User();
            user.setUsername(username);
            user.setPassword(password);
            user.setRealName(username);
            user.setEmail(username + "@quizapp.com");
            user.setRole(role);
            user.setStatus("1");
            userService.registerUser(user);
            log.info("默认用户创建成功: {} (role: {})", username, role.getName());
        } catch (Exception e) {
            log.warn("创建默认用户失败 {}: {}", username, e.getMessage());
        }
    }

    private void fillModuleNames() {
        List<Permission> perms = permissionRepository.findAll();
        int count = 0;
        for (Permission p : perms) {
            if (p.getModuleName() == null || p.getModuleName().isBlank()) {
                String module = p.getModule();
                if (module != null && MODULE_LABELS.containsKey(module)) {
                    p.setModuleName(MODULE_LABELS.get(module));
                    count++;
                }
            }
            if (p.getType() == null || p.getType().isBlank()) {
                String key = p.getKey();
                if (key != null && key.contains(":")) {
                    String action = key.split(":")[1];
                    // view → MENU, 增删改导等 → ACTION, 其余 → API
                    if ("view".equals(action)) {
                        p.setType("MENU");
                    } else if ("create".equals(action) || "edit".equals(action) || "delete".equals(action)
                            || "import".equals(action) || "export".equals(action) || "enable".equals(action)
                            || "manage".equals(action)) {
                        p.setType("ACTION");
                    } else {
                        p.setType("API");
                    }
                } else {
                    p.setType("ACTION");
                }
                count++;
            }
        }
        if (count > 0) {
            permissionRepository.saveAll(perms);
            log.info("已补填 {} 条权限的 type", count);
        }
    }

    private void migrateUserRoles() {
        // 将 sys_user 旧 role 字符串更新到 role_id FK（兼容新库没有旧列的情况）
        try {
            int updated = entityManager.createNativeQuery(
                    "UPDATE sys_user u SET u.role_id = (SELECT r.id FROM sys_role r WHERE UPPER(TRIM(u.role)) = r.name) " +
                    "WHERE u.role_id IS NULL AND u.role IS NOT NULL"
            ).executeUpdate();
            if (updated > 0) {
                log.info("已迁移 {} 条用户的 role_id", updated);
            }
        } catch (Exception e) {
            log.info("旧版 sys_user.role 列不存在（新装数据库），跳过用户角色迁移");
        }
    }

    private void migrateRolePermissions() {
        // 将 sys_role_permission 旧数据迁移到新 FK 列（兼容新库没有旧列的情况）
        try {
            int updated = entityManager.createNativeQuery(
                    "UPDATE sys_role_permission rp SET rp.role_id = (SELECT r.id FROM sys_role r WHERE UPPER(TRIM(rp.role)) = r.name) " +
                    "WHERE rp.role_id IS NULL AND rp.role IS NOT NULL"
            ).executeUpdate();

            int updated2 = entityManager.createNativeQuery(
                    "UPDATE sys_role_permission rp SET rp.permission_id = (SELECT p.id FROM sys_permission p WHERE rp.permission_key = p.`key`) " +
                    "WHERE rp.permission_id IS NULL AND rp.permission_key IS NOT NULL"
            ).executeUpdate();

            if (updated > 0 || updated2 > 0) {
                log.info("已迁移角色权限数据: role_id={}, permission_id={}", updated, updated2);
            }
        } catch (Exception e) {
            log.info("旧版 sys_role_permission 列不存在（新装数据库），跳过角色权限迁移");
        }
    }

    private void migrateUserPermissions() {
        // 将 sys_user_permission 旧数据迁移到新 FK 列（兼容新库没有旧列的情况）
        try {
            int updated = entityManager.createNativeQuery(
                    "UPDATE sys_user_permission up SET up.permission_id = (SELECT p.id FROM sys_permission p WHERE up.permission_key = p.`key`) " +
                    "WHERE up.permission_id IS NULL AND up.permission_key IS NOT NULL"
            ).executeUpdate();

            if (updated > 0) {
                log.info("已迁移 {} 条用户权限覆盖数据", updated);
            }
        } catch (Exception e) {
            log.info("旧版 sys_user_permission 列不存在（新装数据库），跳过用户权限迁移");
        }
    }
}
