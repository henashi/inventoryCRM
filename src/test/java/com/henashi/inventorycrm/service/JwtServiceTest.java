package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class JwtServiceTest {

    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;

    @Test @DisplayName("生成 + 解析 Access Token")
    void generateAndValidateAccessToken() {
        User user = userRepository.findByUsername("admin").orElseThrow();

        // 使用 User 重载
        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
    }

    @Test @DisplayName("生成 + 解析 Refresh Token")
    void generateAndValidateRefreshToken() {
        User user = userRepository.findByUsername("admin").orElseThrow();

        String token = jwtService.generateRefreshToken(user);
        assertThat(token).isNotBlank();

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isTrue();
    }

    @Test @DisplayName("Access/Refresh 类型互斥")
    void tokenTypeExclusive() {
        User user = userRepository.findByUsername("admin").orElseThrow();
        String access = jwtService.generateToken(user);
        String refresh = jwtService.generateRefreshToken(user);

        assertThat(jwtService.isAccessToken(access)).isTrue();
        assertThat(jwtService.isRefreshToken(access)).isFalse();
        assertThat(jwtService.isAccessToken(refresh)).isFalse();
        assertThat(jwtService.isRefreshToken(refresh)).isTrue();
    }

    @Test @DisplayName("Token 版本校验")
    void tokenVersionValidation() {
        User user = userRepository.findByUsername("admin").orElseThrow();
        String token = jwtService.generateToken(user);
        assertThat(jwtService.validateAccessToken(token, user)).isTrue();

        // tokenVersion 增加后旧 token 失效
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        assertThat(jwtService.validateAccessToken(token, user)).isFalse();
    }

    @Test @DisplayName("UserDetails 重载")
    void generateWithUserDetails() {
        User user = userRepository.findByUsername("admin").orElseThrow();
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_ADMIN")
                .build();

        String token = jwtService.generateToken(ud);
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.validateToken(token, ud)).isTrue();
    }
}
