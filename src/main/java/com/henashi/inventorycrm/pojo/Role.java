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
 * 角色实体
 * 替代 User.role 字符串字段，支持动态角色 CRUD
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 角色标识（如 ADMIN / MANAGER / USER），Spring Security 依赖此值生成 ROLE_ 前缀
     */
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private String name;

    /**
     * 角色显示名称（如 "管理员"、"经理"）
     */
    @Column(name = "display_name", length = 50)
    private String displayName;

    /**
     * 角色描述
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 排序号
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 状态：1-启用 0-禁用
     */
    @Column(name = "status", length = 2)
    @Builder.Default
    private String status = "1";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
