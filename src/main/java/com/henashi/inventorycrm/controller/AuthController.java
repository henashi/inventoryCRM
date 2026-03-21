package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.AuthRequest;
import com.henashi.inventorycrm.dto.AuthResponse;
import com.henashi.inventorycrm.dto.RegisterRequest;
import com.henashi.inventorycrm.dto.UserDTO;
import com.henashi.inventorycrm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "认证管理", description = "用户认证相关接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        log.info("用户登录: {}", request.getUsername());

        return authService.authenticate(request);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册: {}", request.getUsername());

        // 验证密码一致性
        request.validatePassword();

        UserDTO user = authService.register(request);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(user.id())
                .toUri();
        return ResponseEntity.created(uri).body(user);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "退出当前登录")
    public ResponseEntity<Void> logout() {
        // 前端清除token 即可
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "获取当前登录用户信息")
    public UserDTO getCurrentUser() {
        return authService.getCurrentUser();
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新令牌", description = "刷新访问令牌")
    public AuthResponse refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("请提供 token");
        }

        return authService.refreshToken(refreshToken);
    }
}