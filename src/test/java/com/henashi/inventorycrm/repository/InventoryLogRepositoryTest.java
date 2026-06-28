package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.dto.InventoryLogTypeStatsDTO;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InventoryLogRepository 自定义 JPQL 查询测试。
 */
@SpringBootTest
@Transactional
class InventoryLogRepositoryTest {

    @Autowired
    private InventoryLogRepository repository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager em;

    private Long productId;

    @BeforeEach
    void setUp() {
        Product p = productRepository.findById(1L).orElseThrow();
        productId = p.getId();

        // 创建两条入库、一条出库流水
        createLog(InventoryLog.LogType.IN, 50, 0, 50, "入库1");
        createLog(InventoryLog.LogType.IN, 30, 50, 80, "入库2");
        createLog(InventoryLog.LogType.OUT, 20, 80, 60, "出库1");
        em.flush();
    }

    private void createLog(InventoryLog.LogType type, int qty, int before, int after, String reason) {
        InventoryLog log = InventoryLog.builder()
                .product(productRepository.findById(1L).orElseThrow())
                .type(type)
                .quantity(qty)
                .beforeStock(before)
                .afterStock(after)
                .reason(reason)
                .operator("admin")
                .status("SUCCESS")
                .createdTime(LocalDateTime.now())
                .build();
        repository.save(log);
    }

    @Test
    @DisplayName("findOutLogsByProductSince — 按商品和时间范围查出库记录")
    void findOutLogsByProductSince() {
        List<InventoryLog> results = repository.findOutLogsByProductSince(
                productId, LocalDateTime.now().minusDays(1));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(InventoryLog.LogType.OUT);
        assertThat(results.get(0).getQuantity()).isEqualTo(20);
    }

    @Test
    @DisplayName("findOutLogsByProductSince — 时间范围外无数据")
    void findOutLogsByProductSinceOutOfRange() {
        List<InventoryLog> results = repository.findOutLogsByProductSince(
                productId, LocalDateTime.now().plusDays(1));
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findAllOutLogsSince — 查询所有商品的出库记录")
    void findAllOutLogsSince() {
        List<InventoryLog> results = repository.findAllOutLogsSince(
                LocalDateTime.now().minusDays(1));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getType()).isEqualTo(InventoryLog.LogType.OUT);
    }

    @Test
    @DisplayName("countStats — 按类型分组统计数量和条数")
    void countStats() {
        List<InventoryLogTypeStatsDTO> stats = repository.countStats();
        // 预期有 IN 和 OUT 两组
        assertThat(stats).isNotEmpty();

        var inStat = stats.stream()
                .filter(s -> s.type() == InventoryLog.LogType.IN)
                .findFirst();
        assertThat(inStat).isPresent();
        assertThat(inStat.get().quantityCount().longValue()).isEqualTo(80); // 50 + 30
        assertThat(inStat.get().count().longValue()).isEqualTo(2);

        var outStat = stats.stream()
                .filter(s -> s.type() == InventoryLog.LogType.OUT)
                .findFirst();
        assertThat(outStat).isPresent();
        assertThat(outStat.get().quantityCount().longValue()).isEqualTo(20);
        assertThat(outStat.get().count().longValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("countByStatus — 按状态统计数量")
    void countByStatus() {
        Long count = repository.countByStatus("SUCCESS");
        assertThat(count).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("findByProductId — 分页查询按商品")
    void findByProductId() {
        var page = repository.findByProductId(productId,
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("findByType — 按类型分页查询")
    void findByType() {
        var page = repository.findByType(InventoryLog.LogType.IN,
                org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
