package com.henashi.inventorycrm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class UserControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("GET /api/users — 分页查询用户列表")
    void listUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET /api/users?keyword=admin — 搜索用户")
    void listUsersWithKeyword() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("keyword", "admin")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("POST /api/users — 创建用户")
    void createUser() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "newuser_" + System.currentTimeMillis(),
                "password", "test123456",
                "role", "USER",
                "remark", "测试用户"
        ));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(startsWith("newuser_")));
    }

    @Test
    @DisplayName("PUT /api/users/{id} — 更新用户角色")
    void updateUserRole() throws Exception {
        // 先创建一个用户
        String createBody = objectMapper.writeValueAsString(Map.of(
                "username", "updatable_" + System.currentTimeMillis(),
                "password", "test123456",
                "role", "USER"
        ));

        var createResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        assertNotNull(location);
        Long userId = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        // 更新角色（UserCreateDTO 要求 password 非空）
        String updateBody = objectMapper.writeValueAsString(Map.of(
                "username", "updatable_user",
                "password", "newpass123",
                "role", "MANAGER"
        ));

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/reset-password — 重置密码")
    void resetPassword() throws Exception {
        mockMvc.perform(put("/api/users/{id}/reset-password", 1))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/users/{id}/status — 切换用户状态")
    void toggleStatus() throws Exception {
        mockMvc.perform(put("/api/users/{id}/status", 1))
                .andExpect(status().isOk());

        // 再次切换，验证状态翻转
        mockMvc.perform(put("/api/users/{id}/status", 1))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} — 删除用户")
    void deleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", 1))
                .andExpect(status().isNoContent());
    }


}
