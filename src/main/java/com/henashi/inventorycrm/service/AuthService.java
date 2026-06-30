package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.AuthRequest;
import com.henashi.inventorycrm.dto.AuthResponse;
import com.henashi.inventorycrm.dto.ChangePasswordRequest;
import com.henashi.inventorycrm.dto.RefreshTokenRequest;
import com.henashi.inventorycrm.dto.RegisterRequest;
import com.henashi.inventorycrm.dto.UpdateProfileRequest;
import com.henashi.inventorycrm.dto.UserDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.exception.SecurityAuthenticationException;
import com.henashi.inventorycrm.exception.UserAlreadyExistsException;
import com.henashi.inventorycrm.mapper.UserMapper;
import com.henashi.inventorycrm.pojo.Role;
import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.repository.RoleRepository;
import com.henashi.inventorycrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
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

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new SecurityAuthenticationException("用户不存在"));

        if ("0".equals(user.getStatus())) {
            throw new SecurityAuthenticationException("用户已被禁用");
        }

        user.setLastLoginAt(LocalDateTime.now());
        if (user.getTokenVersion() == null) {
            user.setTokenVersion(0);
        }
        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userMapper.fromEntity(savedUser))
                .expiresIn(jwtService.getExpirationTime())
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("用户名已存在");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("邮箱已存在");
        }

        Role defaultRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("默认角色 USER 不存在"));

        User user = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .realName(request.getRealName() != null ? request.getRealName().trim() : null)
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .role(defaultRole)
                .status("1")
                .remark(request.getRemark())
                .tokenVersion(0)
                .build();

        User savedUser = userRepository.save(user);

        return userMapper.fromEntity(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser() {
        return userMapper.fromEntity(getCurrentAuthenticatedUser());
    }

    @Transactional
    public UserDTO updateProfile(UpdateProfileRequest request) {
        User user = getCurrentAuthenticatedUser();

        String username = normalizeOptional(request.getUsername());
        if (username != null && !username.equals(user.getUsername())) {
            throw new BusinessException("当前接口不支持修改用户名");
        }

        String realName = normalizeOptional(request.getRealName());
        if (realName != null) {
            user.setRealName(realName);
        }

        String email = normalizeOptional(request.getEmail());
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
                throw new UserAlreadyExistsException("邮箱已存在");
            }
            user.setEmail(email);
        }

        User savedUser = userRepository.save(user);
        return userMapper.fromEntity(savedUser);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentAuthenticatedUser();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new SecurityAuthenticationException("旧密码错误");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(nextTokenVersion(user));
        userRepository.save(user);
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public void logoutCurrentUser() {
        User user = getCurrentAuthenticatedUser();
        user.setTokenVersion(nextTokenVersion(user));
        userRepository.save(user);
        SecurityContextHolder.clearContext();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtService.validateToken(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new SecurityAuthenticationException("无效的刷新令牌");
        }

        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityAuthenticationException("用户不存在"));

        if (!jwtService.validateRefreshToken(refreshToken, user)) {
            throw new SecurityAuthenticationException("刷新令牌已失效，请重新登录");
        }

        if ("0".equals(user.getStatus())) {
            throw new SecurityAuthenticationException("用户已被禁用");
        }

        String newToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(refreshToken)
                .user(userMapper.fromEntity(user))
                .expiresIn(jwtService.getExpirationTime())
                .tokenType("Bearer")
                .build();
    }

    @Transactional(readOnly = true)
    protected User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new SecurityAuthenticationException("未登录或登录状态已失效");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new SecurityAuthenticationException("用户不存在"));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("资料字段不能为空白字符");
        }
        return normalized;
    }

    private int nextTokenVersion(User user) {
        return (user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1;
    }
}
