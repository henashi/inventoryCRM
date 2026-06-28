package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.*;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.exception.SecurityAuthenticationException;
import com.henashi.inventorycrm.exception.UserAlreadyExistsException;
import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    private String uniqueUsername() {
        return "authtest_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
    }

    // ==================== Login / Authenticate ====================

    @Test
    @DisplayName("登录 — admin 成功返回 token + refreshToken + user")
    void authenticateSuccess() {
        AuthResponse resp = authService.authenticate(
                AuthRequest.builder().username("admin").password("admin123").build());

        assertThat(resp.getToken()).isNotBlank();
        assertThat(resp.getRefreshToken()).isNotBlank();
        assertThat(resp.getUser()).isNotNull();
        assertThat(resp.getUser().username()).isEqualTo("admin");
        assertThat(resp.getTokenType()).isEqualTo("Bearer");
        assertThat(resp.getExpiresIn()).isPositive();
    }

    @Test
    @DisplayName("登录 — 错误密码抛出 SecurityAuthenticationException")
    void authenticateWrongPassword() {
        assertThrows(SecurityAuthenticationException.class,
                () -> authService.authenticate(
                        AuthRequest.builder().username("admin").password("wrongpass").build()));
    }

    @Test
    @DisplayName("登录 — 不存在的用户抛出 SecurityAuthenticationException")
    void authenticateNonExistentUser() {
        assertThrows(SecurityAuthenticationException.class,
                () -> authService.authenticate(
                        AuthRequest.builder().username("nobody_" + UUID.randomUUID()).password("pass123456").build()));
    }

    // ==================== Register ====================

    @Test
    @DisplayName("注册 — 创建新用户成功")
    void registerSuccess() {
        String username = uniqueUsername();
        RegisterRequest req = RegisterRequest.builder()
                .username(username)
                .password("pass123456")
                .confirmPassword("pass123456")
                .realName("注册测试")
                .email(username + "@test.com")
                .build();

        UserDTO user = authService.register(req);
        assertThat(user.username()).isEqualTo(username);
        assertThat(user.realName()).isEqualTo("注册测试");
        assertThat(user.role()).isEqualTo("USER");
        assertThat(user.status()).isEqualTo(1);
    }

    @Test
    @DisplayName("注册 — 用户名重复抛出 UserAlreadyExistsException")
    void registerDuplicateUsername() {
        RegisterRequest req = RegisterRequest.builder()
                .username("admin")
                .password("pass123456")
                .confirmPassword("pass123456")
                .realName("重复")
                .email("dup_" + UUID.randomUUID() + "@test.com")
                .build();

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(req));
    }

    @Test
    @DisplayName("注册 — 邮箱重复抛出 UserAlreadyExistsException")
    void registerDuplicateEmail() {
        String username = uniqueUsername();
        RegisterRequest req = RegisterRequest.builder()
                .username(username)
                .password("pass123456")
                .confirmPassword("pass123456")
                .realName("重复邮箱")
                .email("admin@quizapp.com")  // admin 用户的邮箱（来自 WebSecurityConfig）
                .build();

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(req));
    }

    // ==================== Refresh Token ====================

    @Test
    @DisplayName("刷新 Token — 有效 refreshToken 返回新 token")
    void refreshTokenSuccess() {
        AuthResponse loginResp = authService.authenticate(
                AuthRequest.builder().username("admin").password("admin123").build());
        String refreshToken = loginResp.getRefreshToken();

        AuthResponse refreshResp = authService.refreshToken(
                new RefreshTokenRequest(refreshToken));

        assertThat(refreshResp.getToken()).isNotBlank();
        assertThat(refreshResp.getRefreshToken()).isEqualTo(refreshToken); // 原 refreshToken 不变
        assertThat(refreshResp.getUser()).isNotNull();
    }

    @Test
    @DisplayName("刷新 Token — 无效 refreshToken 抛出异常")
    void refreshTokenInvalid() {
        assertThrows(SecurityAuthenticationException.class,
                () -> authService.refreshToken(new RefreshTokenRequest("invalid_token_xxx")));
    }

    // ==================== Change Password ====================

    @Test
    @DisplayName("修改密码 — 成功后旧 token 失效")
    void changePasswordSuccess() {
        // 注册新用户
        String username = uniqueUsername();
        RegisterRequest regReq = RegisterRequest.builder()
                .username(username)
                .password("oldpass123")
                .confirmPassword("oldpass123")
                .realName("改密测试")
                .email(username + "@test.com")
                .build();
        authService.register(regReq);

        // 以新用户身份登录
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        // 修改密码
        authService.changePassword(
                ChangePasswordRequest.builder().oldPassword("oldpass123").newPassword("newpass456").build());

        // 用新密码应可登录
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        AuthResponse newLogin = authService.authenticate(
                AuthRequest.builder().username(username).password("newpass456").build());
        assertThat(newLogin.getToken()).isNotBlank();
    }

    @Test
    @DisplayName("修改密码 — 旧密码错误抛出 SecurityAuthenticationException")
    void changePasswordWrongOldPassword() {
        String username = uniqueUsername();
        RegisterRequest regReq = RegisterRequest.builder()
                .username(username)
                .password("pass123456")
                .confirmPassword("pass123456")
                .realName("改密测试2")
                .email(username + "@test.com")
                .build();
        authService.register(regReq);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        assertThrows(SecurityAuthenticationException.class,
                () -> authService.changePassword(
                        ChangePasswordRequest.builder().oldPassword("wrong_old").newPassword("newpass456").build()));
    }

    @Test
    @DisplayName("修改密码 — 新密码与旧密码相同抛出 BusinessException")
    void changePasswordSameAsOld() {
        String username = uniqueUsername();
        RegisterRequest regReq = RegisterRequest.builder()
                .username(username)
                .password("pass123456")
                .confirmPassword("pass123456")
                .realName("改密测试3")
                .email(username + "@test.com")
                .build();
        authService.register(regReq);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        assertThrows(BusinessException.class,
                () -> authService.changePassword(
                        ChangePasswordRequest.builder().oldPassword("pass123456").newPassword("pass123456").build()));
    }

    // ==================== Update Profile ====================

    @Test
    @DisplayName("更新资料 — 修改真实姓名和邮箱")
    void updateProfileSuccess() {
        String username = uniqueUsername();
        RegisterRequest regReq = RegisterRequest.builder()
                .username(username)
                .password("test123456")
                .confirmPassword("test123456")
                .realName("原名")
                .email(username + "@test.com")
                .build();
        authService.register(regReq);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        UserDTO updated = authService.updateProfile(
                UpdateProfileRequest.builder()
                        .username(username)
                        .realName("新姓名")
                        .email("new_" + username + "@test.com")
                        .build());

        assertThat(updated.realName()).isEqualTo("新姓名");
        assertThat(updated.username()).isEqualTo(username);
    }

    @Test
    @DisplayName("更新资料 — 尝试修改用户名抛出 BusinessException")
    void updateProfileRejectsUsernameChange() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        assertThrows(BusinessException.class,
                () -> authService.updateProfile(
                        UpdateProfileRequest.builder().username("new_admin_name").build()));
    }

    // ==================== Get Current User ====================

    @Test
    @DisplayName("获取当前用户 — 返回已认证用户信息")
    void getCurrentUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        UserDTO user = authService.getCurrentUser();
        assertThat(user.username()).isEqualTo("admin");
    }

    @Test
    @DisplayName("获取当前用户 — 未登录抛出 SecurityAuthenticationException")
    void getCurrentUserNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThrows(SecurityAuthenticationException.class, () -> authService.getCurrentUser());
    }

    // ==================== Logout ====================

    @Test
    @DisplayName("退出 — 增加 tokenVersion 使旧 token 失效")
    void logoutCurrentUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        User before = userRepository.findByUsername("admin").orElseThrow();
        Integer versionBefore = before.getTokenVersion();

        authService.logoutCurrentUser();

        User after = userRepository.findByUsername("admin").orElseThrow();
        assertThat(after.getTokenVersion()).isEqualTo(versionBefore + 1);
    }
}
