package com.henashi.inventorycrm.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CustomerController 文件上传 + CSV 导出测试。
 */
@SpringBootTest
@Transactional
class CustomerControllerImportTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("POST /api/customers/import — 正常 CSV 导入")
    void importCustomers() throws Exception {
        String csv = "name,phone,email\n张三,13800000001,zhangsan@test.com\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "customers.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/customers/import")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1));
    }

    @Test
    @DisplayName("POST /api/customers/import — 空文件返回错误")
    void importEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/customers/import")
                        .file(emptyFile))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/customers/import — 非 CSV 文件被拒")
    void importNonCsvFile() throws Exception {
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "data.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/customers/import")
                        .file(txtFile))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /api/customers/export — CSV 导出")
    void exportCustomers() throws Exception {
        mockMvc.perform(get("/api/customers/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().string(containsString("客户ID")));
    }

    @Test
    @DisplayName("GET /api/customers/import/template — 返回导入模板元信息")
    void importTemplate() throws Exception {
        mockMvc.perform(get("/api/customers/import/template"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateFields").isArray());
    }
}
