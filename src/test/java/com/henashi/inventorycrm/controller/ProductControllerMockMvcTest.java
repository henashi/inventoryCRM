package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.repository.InventoryLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProductController 特殊逻辑测试 — 库存变更 + @InventoryAudit AOP。
 */
@SpringBootTest
@Transactional
class ProductControllerMockMvcTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/stock — 库存出库成功，库存减少")
    void stockOut() throws Exception {
        String body = "{\"type\":\"OUT\",\"quantity\":5,\"reason\":\"测试出库\"}";

        mockMvc.perform(patch("/api/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.currentStock").value(95));
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/stock — 库存入库成功，库存增加")
    void stockIn() throws Exception {
        String body = "{\"type\":\"IN\",\"quantity\":10,\"reason\":\"测试入库\"}";

        mockMvc.perform(patch("/api/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock").value(110));
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/stock — 库存不足应返回 400")
    void stockOutInsufficient() throws Exception {
        String body = "{\"type\":\"OUT\",\"quantity\":999,\"reason\":\"测试超量出库\"}";

        mockMvc.perform(patch("/api/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/products/{id}/stock — @InventoryAudit 触发 inventory_log 记录")
    void stockChangeTriggersAuditLog() throws Exception {
        String body = "{\"type\":\"OUT\",\"quantity\":3,\"reason\":\"审计测试出库\"}";

        mockMvc.perform(patch("/api/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // 验证 AOP 切面写入了库存流水
        List<InventoryLog> logs = inventoryLogRepository.findAll();
        assertFalse(logs.isEmpty(), "库存变更应触发 @InventoryAudit 写入 inventory_log");

        boolean hasOutLog = logs.stream()
                .anyMatch(log -> log.getType() == InventoryLog.LogType.OUT
                        && log.getProduct() != null
                        && log.getProduct().getId() == 1L);
        org.assertj.core.api.Assertions.assertThat(hasOutLog)
                .as("应存在 product.id=1 且 type=OUT 的库存流水记录")
                .isTrue();
    }
}
