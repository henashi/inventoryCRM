package com.henashi.inventorycrm.service;

import ch.qos.logback.core.util.StringUtil;
import com.henashi.inventorycrm.dto.UserCreateDTO;
import com.henashi.inventorycrm.dto.UserDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.UserMapper;
import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
//    private final UserDetailsServiceImpl userDetailsService;
//    private final Map<String, User> map = new ConcurrentHashMap<>(16);

    public UserDTO findUserDTOById(Long userId) {
        log.debug("查询用户详情: id={}", userId);
        if (userId == null || userId <= 0) {
            log.warn("用户ID无效: id={}", userId);
            throw new BusinessException("用户ID无效");
        }
        return userRepository.findById(userId)
                .map(user -> {
                    log.info("找到用户: {}", user.getUsername());
                    return userMapper.fromEntity(user);
                })
                .orElseThrow(() -> {
                    log.warn("用户不存在: id={}", userId);
                    return new BusinessException("USER_NOT_FOUND",
                            String.format("用户(ID: %d)不存在", userId));
                });
    }

    public UserDTO findByUsername(String username) {
        if (StringUtil.isNullOrEmpty(username)) {
            log.warn("用户名不能为空");
            throw new BusinessException("用户名不能为空");
        }
        log.debug("根据用户名查询用户: username={}", username);
        return userRepository.findByUsername((username))
                .map(userMapper::fromEntity)
                .orElseThrow(() -> {
                    log.warn("用户不存在: username={}", username);
                    return new BusinessException("USER_NOT_FOUND",
                            String.format("用户 %s 不存在", username));
                });
    }

    @Transactional
    public UserDTO saveUser(UserCreateDTO userCreateDTO) {
        log.debug("创建用户: {}", userCreateDTO.username());

        // 验证用户名是否重复
        if (userRepository.existsByUsername((userCreateDTO.username()))) {
            throw new BusinessException("USERNAME_EXISTS",
                    String.format("用户名 %s 已存在", userCreateDTO.username()));
        }

        // 密码加密
        String encodedPassword = passwordEncoder.encode(userCreateDTO.password());

        User user = userMapper.createToEntity(userCreateDTO);
        user.setPassword(encodedPassword);
        // 默认启用
        user.setStatus(1);

        User saved = userRepository.save(user);
        log.info("用户创建成功: {} ", saved.getUsername());

        return userMapper.fromEntity(saved);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserCreateDTO dto) {
        if (id == null || id <= 0L) {
            log.warn("更新用户信息异常: {}", dto);
            throw new BusinessException("用户ID无效");
        }

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("未找到用户: {}", id);
                    return new BusinessException("USER_NOT_FOUND",
                            String.format("用户(ID: %d)不存在", id));
                });

        log.info("找到用户信息: {}", existingUser.getUsername());

        // 验证用户名是否重复（如果修改了用户名）
        if (dto.username() != null && !existingUser.getUsername().equals(dto.username())) {
            if (userRepository.existsByUsername(dto.username())) {
                throw new BusinessException("USERNAME_EXISTS",
                        String.format("用户名 %s 已存在", dto.username()));
            }
        }

        // 只更新允许修改的字段
        updateAllowedFields(existingUser, dto);

        User saved = userRepository.save(existingUser);
        log.info("用户更新成功: {}", saved.getUsername());

        return userMapper.fromEntity(saved);
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null || id <= 0L) {
            log.warn("用户ID异常: {}", id);
            throw new BusinessException("用户ID无效");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("根据ID未找到用户: {}", id);
                    return new BusinessException("USER_NOT_FOUND",
                            String.format("用户(ID: %d)不存在", id));
                });

        if (user.getStatus() == 0) {
            log.warn("该用户(ID: {})已停用", id);
            throw new BusinessException("USER_ALREADY_DISABLED",
                    String.format("用户(ID: %d)已停用", id));
        }

        user.setStatus(0); // 停用
        userRepository.save(user);
        log.info("用户停用成功: {} ", user.getUsername());
    }

    private void updateAllowedFields(User user, UserCreateDTO dto) {
        if (dto.username() != null && !user.getUsername().equals(dto.username())) {
            user.setUsername(dto.username());
        }
        if (dto.role() != null) {
            user.setRole(dto.role());
        }
        if (dto.password() != null && !dto.password().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }
        if (dto.remark() != null) {
            user.setRemark(dto.remark());
        }
    }

    @Transactional
    public void updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));
        if (StringUtil.isNullOrEmpty(newPassword)) {
            throw new BusinessException("PASSWORD_EMPTY", "新密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("用户密码更新成功: {} ", user.getUsername());
    }

    @Transactional
    public void registerUser(User user) throws BusinessException {
        if (user == null
                || user.getUsername() == null
                || user.getUsername().trim().isEmpty()) {
            throw new BusinessException("INVALID_INPUT", "用户名无效");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new BusinessException("INVALID_INPUT", "密码不能为空");
        }

        // 先做快速存在性检查（非绝对安全，下面通过 DB 约束兜底）
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new BusinessException("USERNAME_EXISTS", String.format("用户名 %s 已存在", user.getUsername()));
        }

        // 统一在服务端编码密码（约定：registerUser 接受明文密码）
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }

        try {
            userRepository.save(user);
            log.info("用户注册成功: {}", user.getUsername());
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // 处理并发导致的唯一约束冲突
            log.warn("注册失败，用户名可能已存在: {}", user.getUsername());
            throw new BusinessException("USERNAME_EXISTS", String.format("用户名 %s 已存在", user.getUsername()));
        }
    }
//    public void registerUser(User user) throws BusinessException{
//        if (map.containsKey(user.getUsername())) {
//            throw new BusinessException("Username is exists");
//        }
//        else {
//            user.setPassword(passwordEncoder.encode(user.getPassword()));
//            map.put(user.getUsername(), user);
//        }
//    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.getUserByUsername(username);
    }
}