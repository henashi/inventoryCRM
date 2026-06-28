package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.InventoryChangeDTO;
import com.henashi.inventorycrm.dto.InventoryDTO;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InventoryMapper 自定义表达式单元测试。
 * MapStruct 生成代码 + 自定义 default 方法，无需 Spring 上下文。
 */
class InventoryMapperTest {

    private final InventoryMapper mapper = Mappers.getMapper(InventoryMapper.class);

    // ========== resolveStatus ==========

    @Test
    @DisplayName("resolveStatus — \"1\" → 1")
    void resolveStatusNormal() {
        assertThat(mapper.resolveStatus("1")).isEqualTo(1);
    }

    @Test
    @DisplayName("resolveStatus — \"0\" → 0")
    void resolveStatusDisabled() {
        assertThat(mapper.resolveStatus("0")).isEqualTo(0);
    }

    @Test
    @DisplayName("resolveStatus — null → 1")
    void resolveStatusNull() {
        assertThat(mapper.resolveStatus(null)).isEqualTo(1);
    }

    @Test
    @DisplayName("resolveStatus — 空串 → 1")
    void resolveStatusBlank() {
        assertThat(mapper.resolveStatus("")).isEqualTo(1);
    }

    // ========== resolveLastUpdateTime ==========

    @Test
    @DisplayName("resolveLastUpdateTime — contentUpdatedTime 优先")
    void lastUpdateTimeContentFirst() {
        Product p = new Product();
        LocalDateTime content = LocalDateTime.of(2025, 6, 1, 10, 0);
        LocalDateTime status = LocalDateTime.of(2025, 5, 1, 10, 0);
        LocalDateTime created = LocalDateTime.of(2025, 4, 1, 10, 0);
        p.setContentUpdatedTime(content);
        p.setStatusUpdatedTime(status);
        p.setCreatedTime(created);
        assertThat(mapper.resolveLastUpdateTime(p)).isEqualTo(content);
    }

    @Test
    @DisplayName("resolveLastUpdateTime — content 为 null 时取 statusUpdatedTime")
    void lastUpdateTimeStatusFallback() {
        Product p = new Product();
        LocalDateTime status = LocalDateTime.of(2025, 5, 1, 10, 0);
        LocalDateTime created = LocalDateTime.of(2025, 4, 1, 10, 0);
        p.setStatusUpdatedTime(status);
        p.setCreatedTime(created);
        assertThat(mapper.resolveLastUpdateTime(p)).isEqualTo(status);
    }

    @Test
    @DisplayName("resolveLastUpdateTime — 全部为 null 时取 createdTime")
    void lastUpdateTimeCreatedFallback() {
        Product p = new Product();
        LocalDateTime created = LocalDateTime.of(2025, 4, 1, 10, 0);
        p.setCreatedTime(created);
        assertThat(mapper.resolveLastUpdateTime(p)).isEqualTo(created);
    }

    // ========== resolveChangeType ==========

    @Test
    @DisplayName("resolveChangeType — IN → \"in\"")
    void changeTypeIn() {
        assertThat(mapper.resolveChangeType(InventoryLog.LogType.IN)).isEqualTo("in");
    }

    @Test
    @DisplayName("resolveChangeType — CREATE → \"in\"")
    void changeTypeCreate() {
        assertThat(mapper.resolveChangeType(InventoryLog.LogType.CREATE)).isEqualTo("in");
    }

    @Test
    @DisplayName("resolveChangeType — OUT → \"out\"")
    void changeTypeOut() {
        assertThat(mapper.resolveChangeType(InventoryLog.LogType.OUT)).isEqualTo("out");
    }

    @Test
    @DisplayName("resolveChangeType — ADJUST → \"adjust\"")
    void changeTypeAdjust() {
        assertThat(mapper.resolveChangeType(InventoryLog.LogType.ADJUST)).isEqualTo("adjust");
    }

    @Test
    @DisplayName("resolveChangeType — PARAM → \"adjust\"")
    void changeTypeParam() {
        assertThat(mapper.resolveChangeType(InventoryLog.LogType.PARAM)).isEqualTo("adjust");
    }

    @Test
    @DisplayName("resolveChangeType — null → \"adjust\"")
    void changeTypeNull() {
        assertThat(mapper.resolveChangeType(null)).isEqualTo("adjust");
    }

    // ========== toInventoryDTO ==========

    @Test
    @DisplayName("toInventoryDTO — 基本字段映射")
    void toInventoryDTOBasic() {
        Product p = new Product();
        p.setId(42L);
        p.setCode("P001");
        p.setName("测试商品");
        p.setCategory("电子");
        p.setStatus("1");
        p.setCurrentStock(100);
        p.setSafeStock(10);

        InventoryDTO dto = mapper.toInventoryDTO(p);
        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.productId()).isEqualTo(42L);
        assertThat(dto.productCode()).isEqualTo("P001");
        assertThat(dto.productName()).isEqualTo("测试商品");
        assertThat(dto.category()).isEqualTo("电子");
        assertThat(dto.status()).isEqualTo(1);
    }

    @Test
    @DisplayName("toInventoryDTO — 低库存和缺货状态")
    void toInventoryDTOStockFlags() {
        Product p = new Product();
        p.setCurrentStock(0);
        p.setSafeStock(10);
        // isStockLow() = currentStock < safeStock = 0 < 10 = true
        // isOutOfStock() = currentStock <= 0 = true

        InventoryDTO dto = mapper.toInventoryDTO(p);
        assertThat(dto.lowStock()).isTrue();
        assertThat(dto.outOfStock()).isTrue();
    }

    // ========== toInventoryChangeDTO ==========

    @Test
    @DisplayName("toInventoryChangeDTO — 基本字段映射")
    void toInventoryChangeDTOBasic() {
        Product p = new Product();
        p.setId(1L);
        p.setName("测试商品");

        InventoryLog log = InventoryLog.builder()
                .id(10L)
                .product(p)
                .type(InventoryLog.LogType.OUT)
                .quantity(5)
                .beforeStock(100)
                .afterStock(95)
                .reason("测试出库")
                .operator("admin")
                .remark("备注")
                .createdTime(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();

        InventoryChangeDTO dto = mapper.toInventoryChangeDTO(log);
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.inventoryId()).isEqualTo(1L);
        assertThat(dto.productId()).isEqualTo(1L);
        assertThat(dto.productName()).isEqualTo("测试商品");
        assertThat(dto.changeType()).isEqualTo("out");
        assertThat(dto.changeQuantity()).isEqualTo(5);
        assertThat(dto.beforeQuantity()).isEqualTo(100);
        assertThat(dto.afterQuantity()).isEqualTo(95);
        assertThat(dto.reason()).isEqualTo("测试出库");
        assertThat(dto.operator()).isEqualTo("admin");
        assertThat(dto.remark()).isEqualTo("备注");
    }
}
