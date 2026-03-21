package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.AuthRequest;
import com.henashi.inventorycrm.dto.AuthResponse;
import com.henashi.inventorycrm.dto.RegisterRequest;
import com.henashi.inventorycrm.dto.UserDTO;
import com.henashi.inventorycrm.exception.SecurityAuthenticationException;
import com.henashi.inventorycrm.mapper.UserMapper;
import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.exception.UserAlreadyExistsException;
import com.henashi.inventorycrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        // 认证
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        }
        catch (DisabledException e) {
            throw new SecurityAuthenticationException("用户已被禁用");
        }
        catch (BadCredentialsException e) {
            throw new SecurityAuthenticationException("用户名或密码错误");
        }

        // 获取用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new SecurityAuthenticationException("用户不存在"));

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new SecurityAuthenticationException("用户已被禁用");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 生成token
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // 返回响应
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userMapper.fromEntity(user))
                .expiresIn(jwtService.getExpirationTime())
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public UserDTO register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("邮箱已存在");
        }

        // 创建用户
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role("USER")  // 默认角色
                .status(1)     // 默认启用
                .build();

        User savedUser = userRepository.save(user);

        return userMapper.fromEntity(savedUser);
    }

    public UserDTO getCurrentUser() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext()
                        .getAuthentication())
                .getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityAuthenticationException("用户不存在"));

        return userMapper.fromEntity(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.validateToken(refreshToken)) {
            throw new SecurityAuthenticationException("无效的刷新令牌");
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityAuthenticationException("用户不存在"));

        String newToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(refreshToken)  // 返回相同的刷新令牌
                .user(userMapper.fromEntity(user))
                .expiresIn(jwtService.getExpirationTime())
                .tokenType("Bearer")
                .build();
    }
}