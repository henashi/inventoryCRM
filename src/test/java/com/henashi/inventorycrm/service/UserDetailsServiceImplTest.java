package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class UserDetailsServiceImplTest {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("loadUserByUsername — admin 用户成功加载")
    void loadByUsernameSuccess() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails custom = (CustomUserDetails) userDetails;
        assertThat(custom.getUsername()).isEqualTo("admin");
        assertThat(custom.getAuthorities()).isNotEmpty();
    }

    @Test
    @DisplayName("loadUserByUsername — 不存在的用户名抛出 UsernameNotFoundException")
    void loadByUsernameNotFound() {
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("nonexistent_user_" + System.currentTimeMillis()));
    }

    @Test
    @DisplayName("loadUserById — 有效 ID 成功加载")
    void loadByIdSuccess() {
        // admin 用户的 ID 通常为 1（由 data.sql 保证）
        UserDetails userDetails = userDetailsService.loadUserById(1L);

        assertThat(userDetails).isInstanceOf(CustomUserDetails.class);
        CustomUserDetails custom = (CustomUserDetails) userDetails;
        assertThat(custom.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("loadUserById — 不存在的 ID 抛出 UsernameNotFoundException")
    void loadByIdNotFound() {
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserById(99999L));
    }

    @Test
    @DisplayName("loadUserByUsername — 返回的用户包含角色权限")
    void loadByUsernameHasAuthorities() {
        CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername("admin");

        assertThat(userDetails.getAuthorities()).isNotEmpty();
        // admin 用户至少有一个角色
        assertThat(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().startsWith("ROLE_"))).isTrue();
    }
}
