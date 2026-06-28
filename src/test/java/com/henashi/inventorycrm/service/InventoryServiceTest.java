package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.InventoryAdjustDTO;
import com.henashi.inventorycrm.dto.InventoryDTO;
import com.henashi.inventorycrm.dto.InventoryInDTO;
import com.henashi.inventorycrm.dto.InventoryOutDTO;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.InventoryLogRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 库存服务集成测试
 */
@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    private static final Long PID = 1L;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        Product p = productRepository.findById(PID).orElseThrow();
        p.setCurrentStock(100);
        p.setSafeStock(10);
        productRepository.save(p);
    }

    @Test
    @DisplayName("入库 — 增加库存并记录流水")
    void stockIn() {
        InventoryDTO r = inventoryService.stockIn(new InventoryInDTO(PID, 50, null, "测试入库", null));
        assertThat(r.currentStock()).isEqualTo(150);

        // 最近一条流水
        org.springframework.data.domain.Page<InventoryLog> logPage =
                inventoryLogRepository.findByProductId(PID, PageRequest.of(0, 1));
        InventoryLog latest = logPage.getContent().get(0);
        assertThat(latest.getType()).isEqualTo(InventoryLog.LogType.IN);
        assertThat(latest.getQuantity()).isEqualTo(50);
        assertThat(latest.getBeforeStock()).isEqualTo(100);
        assertThat(latest.getAfterStock()).isEqualTo(150);
    }

    @Test
    @DisplayName("入库 — 数量为 0 抛出异常")
    void stockInZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
                inventoryService.stockIn(new InventoryInDTO(PID, 0, null, "零数量", null)));
    }

    @Test
    @DisplayName("出库 — 扣减库存")
    void stockOut() {
        InventoryDTO r = inventoryService.stockOut(new InventoryOutDTO(PID, 30, null, "测试出库", null));
        assertThat(r.currentStock()).isEqualTo(70);
    }

    @Test
    @DisplayName("出库 — 库存不足抛出 IllegalStateException")
    void stockOutInsufficientStock() {
        assertThrows(IllegalStateException.class, () ->
                inventoryService.stockOut(new InventoryOutDTO(PID, 999, null, "超额", null)));
    }

    @Test
    @DisplayName("调整 — 调整库存为指定值")
    void adjustStock() {
        InventoryDTO r = inventoryService.adjustStock(PID, new InventoryAdjustDTO(80, "盘点", null));
        assertThat(r.currentStock()).isEqualTo(80);
    }

    @Test
    @DisplayName("预警 — 低库存可被检测")
    void stockAlerts() {
        Product p = productRepository.findById(PID).orElseThrow();
        p.setCurrentStock(3);
        p.setSafeStock(10);
        productRepository.save(p);

        List<InventoryDTO> alerts = inventoryService.findAlerts(null);
        assertThat(alerts).anyMatch(a -> a.productId().equals(PID));
    }

    @Test
    @DisplayName("查询 — 分页返回列表")
    void findInventories() {
        java.util.List<InventoryDTO> list = inventoryService.findInventories(0, 5, null, null, null, null, null, null).getContent();
        assertThat(list).isNotEmpty();
    }

    @Test
    @DisplayName("查询 — 按商品名搜索")
    void findInventoriesWithKeyword() {
        java.util.List<InventoryDTO> list = inventoryService.findInventories(0, 5, "测试商品", null, null, null, null, null).getContent();
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).productName()).contains("测试商品");
    }

    @Test
    @DisplayName("入库 — 负数数量抛出异常")
    void stockInNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
                inventoryService.stockIn(new InventoryInDTO(PID, -1, null, "负数", null)));
    }

    @Test
    @DisplayName("调整 — 负数抛出异常")
    void adjustStockNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                inventoryService.adjustStock(PID, new InventoryAdjustDTO(-1, "负数盘点", null)));
    }

    @Test
    @DisplayName("详情 — 返回基本信息及最近变更")
    void findInventoryDetail() {
        var detail = inventoryService.findInventoryDetail(PID);
        assertThat(detail).isNotNull();
        assertThat(detail.productId()).isEqualTo(PID);
        assertThat(detail.productName()).isNotBlank();
        assertThat(detail.recentChanges()).isNotNull();
    }

    @Test
    @DisplayName("历史 — 分页查询库存变更记录")
    void findInventoryHistory() {
        // 先做一笔入库产生流水
        inventoryService.stockIn(new InventoryInDTO(PID, 10, null, "产生流水", null));
        var page = inventoryService.findInventoryHistory(0, 5, PID, null, null, null, null);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("预警 — 按自定义阈值查询")
    void findAlertsWithCustomThreshold() {
        Product p = productRepository.findById(PID).orElseThrow();
        p.setCurrentStock(5);
        p.setSafeStock(10);
        productRepository.save(p);

        // 设置一个比当前库存高的阈值，应命中
        List<InventoryDTO> alerts = inventoryService.findAlerts(8);
        assertThat(alerts).anyMatch(a -> a.productId().equals(PID));

        // 设置一个比当前库存低的阈值，不应命中
        List<InventoryDTO> noAlerts = inventoryService.findAlerts(2);
        assertThat(noAlerts).noneMatch(a -> a.productId().equals(PID));
    }

    @Test
    @DisplayName("导出 — CSV 格式含 BOM 头")
    void exportInventories() {
        byte[] csv = inventoryService.exportInventories(null, null, null, null, null, null);
        assertThat(csv).isNotEmpty();
        String content = new String(csv, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(content).startsWith("\uFEFF");
        assertThat(content).contains("商品编码");
        assertThat(content).contains("测试商品");
    }
}
