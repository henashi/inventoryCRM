package com.henashi.inventorycrm.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 权限实体
 * 替代 data_dict 中 group_code='PERMISSION' 的权限定义
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 权限标识（如 customers:delete）
     */
    @Column(name = "`key`", nullable = false, unique = true, length = 100)
    private String key;

    /**
     * 权限名称（如 "删除客户"）
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 所属模块（如 customers, products, inventory）
     */
    @Column(name = "module", length = 50)
    private String module;

    /**
     * 模块中文显示名（如 "客户管理"），由前端分组展示
     */
    @Column(name = "module_name", length = 50)
    private String moduleName;

    /**
     * 权限描述
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 默认角色 JSON 数组（如 ["ADMIN","MANAGER"]），从 data_dict 迁移而来
     */
    @Column(name = "default_roles", length = 500)
    private String defaultRoles;

    /**
     * 权限类型：MENU（菜单）、API（接口）、ACTION（操作）
     */
    @Column(name = "type", length = 10)
    @Builder.Default
    private String type = "ACTION";

    /**
     * 排序号
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
