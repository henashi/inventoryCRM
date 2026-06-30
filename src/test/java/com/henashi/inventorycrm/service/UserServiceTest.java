package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.UserCreateDTO;
import com.henashi.inventorycrm.dto.UserDTO;
import com.henashi.inventorycrm.exception.UserAlreadyExistsException;
import com.henashi.inventorycrm.pojo.Role;
import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.repository.RoleRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test @DisplayName("创建用户")
    void createUser() {
        // UserCreateDTO: (username, password, role, remark)
        UserCreateDTO dto = new UserCreateDTO("newuser", "pass123", "USER", "新用户");
        UserDTO r = userService.saveUser(dto);
        assertThat(r.id()).isNotNull();
        assertThat(r.username()).isEqualTo("newuser");
    }

    @Test @DisplayName("创建用户 — 用户名重复抛异常")
    void createUserDuplicate() {
        UserCreateDTO dto = new UserCreateDTO("admin", "pass123", "USER", "重复");
        // UserService 抛出 BusinessException 而非 UserAlreadyExistsException
        assertThrows(RuntimeException.class, () -> userService.saveUser(dto));
    }

    @Test @DisplayName("查询 — 根据 ID")
    void findById() {
        UserDTO u = userService.findUserDTOById(1L);
        assertThat(u).isNotNull();
    }

    @Test @DisplayName("查询 — 根据用户名")
    void findByUsername() {
        UserDTO u = userService.findByUsername("admin");
        assertThat(u.username()).isEqualTo("admin");
    }

    @Test @DisplayName("查询 — Optional 版")
    void getUserByUsername() {
        Optional<User> u = userService.getUserByUsername("admin");
        assertThat(u).isPresent();
        assertThat(u.get().getUsername()).isEqualTo("admin");
    }

    @Test @DisplayName("更新用户 — 修改角色和备注")
    void updateUser() {
        UserCreateDTO dto = new UserCreateDTO(null, null, "MANAGER", "新备注");
        UserDTO r = userService.updateUser(1L, dto);
        assertThat(r.role()).isEqualTo("MANAGER");
    }

    @Test @DisplayName("软删除")
    void deleteUser() {
        userService.deleteById(1L);
        assertThrows(RuntimeException.class, () -> userService.findUserDTOById(1L));
    }

    @Test @DisplayName("更新密码")
    void updatePassword() {
        userService.updatePassword(1L, "newpass456");
    }

    @Test @DisplayName("registerUser — 编码密码并保存")
    void registerUser() {
        Role defaultRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").displayName("普通用户").status("1").build()));
        User user = new User();
        user.setUsername("reg_" + System.currentTimeMillis());
        user.setPassword("rawPass123");
        user.setRealName("注册用户");
        user.setEmail("reg@test.com");
        user.setRole(defaultRole);
        user.setStatus("1");

        userService.registerUser(user);
        assertThat(user.getId()).isNotNull();
        assertThat(user.getPassword()).isNotEqualTo("rawPass123");
    }
}
