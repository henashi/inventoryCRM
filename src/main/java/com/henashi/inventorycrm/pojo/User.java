package com.henashi.inventorycrm.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * 系统用户实体（用于登录管理后台）
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sys_user")
@SQLRestriction(value = "is_deleted = false")
@SQLDelete(sql = "update sys_user set is_deleted = true where id = ?")
public class User extends BaseEntity implements UserDetails{

    /**
     * 用户名
     */
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    /**
     * 密码（加密存储）
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /**
     * 邮件
     */
    @Column(name = "email", length = 50)
    private String email;

    /**
     * 角色：ADMIN-管理员 USER-普通用户
     */
    @Column(name = "role", length = 20)
    private String role = "USER";

    /**
     * 用户状态：1-正常 0-锁定
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "remark", length = 200)
    private String remark;
    // UserDetails 接口实现

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == 1;
    }
}
