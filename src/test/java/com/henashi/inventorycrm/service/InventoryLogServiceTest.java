package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.InventoryLogDTO;
import com.henashi.inventorycrm.dto.InventoryLogStatsDTO;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.InventoryLogRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InventoryLogServiceTest {

    @Autowired private InventoryLogService inventoryLogService;
    @Autowired private InventoryLogRepository inventoryLogRepository;
    @Autowired private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        Product p = productRepository.findById(1L).orElseThrow();
        InventoryLog log = InventoryLog.builder()
                .product(p)
                .type(InventoryLog.LogType.IN)
                .quantity(10)
                .beforeStock(100)
                .afterStock(110)
                .operator("admin")
                .reason("测试准备")
                .build();
        inventoryLogRepository.save(log);
    }

    @Test @DisplayName("查询 — 分页列表")
    void findPage() {
        Page<InventoryLogDTO> page = inventoryLogService.findPage(0, 10, null, null, null, null, null);
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test @DisplayName("查询 — 按商品 ID")
    void findByProductId() {
        Page<InventoryLogDTO> page = inventoryLogService.getLogsByProductId(1L, PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test @DisplayName("查询 — 按类型")
    void findByType() {
        Page<InventoryLogDTO> page = inventoryLogService.getLogsByType("IN", PageRequest.of(0, 10));
        assertThat(page).isNotNull();
    }

    @Test @DisplayName("统计")
    void countStats() {
        InventoryLogStatsDTO stats = inventoryLogService.countStats();
        assertThat(stats).isNotNull();
        assertThat(stats.inCount() + stats.outCount()).isGreaterThanOrEqualTo(0);
    }
}
