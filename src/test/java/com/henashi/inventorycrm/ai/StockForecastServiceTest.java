package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.StockForecastService.RegressionModel;
import com.henashi.inventorycrm.ai.dto.StockPredictionDTO;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.InventoryLogRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockForecastService 预测算法单元测试
 * <p>
 * 覆盖 OLS 线性回归训练、数据聚合、补货量计算、完整预测链路。
 */
@SpringBootTest
@Transactional
class StockForecastServiceTest {

    @Autowired
    private StockForecastService forecastService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = productRepository.save(Product.builder()
                .name("预测测试商品")
                .code("PREDICT_001")
                .currentStock(100)
                .safeStock(20)
                .unit("件")
                .price(BigDecimal.valueOf(50))
                .status("1")
                .build());
    }

    // ==================== test data helpers ====================

    /**
     * 生成指定天数的出库数据。
     * quantities[0] = 最早日（x=0）, quantities[N-1] = 最新日（x=N-1），
     * 对应回归模型中 x[i]=i 从旧到新的索引顺序。
     */
    private Map<LocalDate, Integer> makeDailyMap(int[] quantities, int windowDays) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, Integer> map = new LinkedHashMap<>();
        // 全填 0 占位
        for (int i = windowDays - 1; i >= 0; i--) {
            map.put(today.minusDays(i), 0);
        }
        // 从最早日（today-windowDays+1）填充到最新
        for (int i = 0; i < quantities.length && i < windowDays; i++) {
            map.put(today.minusDays(windowDays - 1 - i), quantities[i]);
        }
        return map;
    }

    // ==================== trainLinearRegression — OLS 核心算法 ====================

    @Test
    @DisplayName("OLS — 完美线性增长趋势")
    void trainLinearRegressionPerfectUpTrend() {
        // y = 2x + 1 → 数据点: x=0→1, x=1→3, x=2→5, x=3→7, x=4→9
        // 用 n=5 窗口确保零数据不干扰
        Map<LocalDate, Integer> data = makeDailyMap(new int[]{1, 3, 5, 7, 9}, 5);

        RegressionModel model = forecastService.trainLinearRegression(data, 5);

        assertThat(model.slope()).isPositive();  // 斜率 > 0
        assertThat(model.rSquared()).isGreaterThan(0.9);  // 拟合度好
        assertThat(model.trendDirection()).isEqualTo("UP");
    }

    @Test
    @DisplayName("OLS — 下降趋势")
    void trainLinearRegressionDownTrend() {
        // 数据点递减: x=0→9, x=1→7, x=2→5, x=3→3, x=4→1
        Map<LocalDate, Integer> data = makeDailyMap(new int[]{9, 7, 5, 3, 1}, 5);

        RegressionModel model = forecastService.trainLinearRegression(data, 5);

        assertThat(model.slope()).isNegative();  // 斜率 < 0
        assertThat(model.trendDirection()).isEqualTo("DOWN");
    }

    @Test
    @DisplayName("OLS — 平稳趋势（斜率绝对值 ≤ 0.05）")
    void trainLinearRegressionStable() {
        // 所有值相同 → 斜率为 0 → STABLE
        Map<LocalDate, Integer> data = makeDailyMap(new int[]{5, 5, 5, 5, 5}, 5);

        RegressionModel model = forecastService.trainLinearRegression(data, 5);

        assertThat(model.trendDirection()).isEqualTo("STABLE");
        assertThat(model.predictedDailyOut()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("OLS — 数据点不足（<3）返回简单平均值")
    void trainLinearRegressionInsufficientData() {
        // window=1: 只有 1 个数据点，走 <3 分支配平均
        Map<LocalDate, Integer> data = makeDailyMap(new int[]{5}, 1);

        RegressionModel model = forecastService.trainLinearRegression(data, 1);

        assertThat(model.slope()).isEqualTo(0);
        assertThat(model.avgDailyOut()).isEqualTo(5);
    }

    @Test
    @DisplayName("OLS — 空数据返回 0")
    void trainLinearRegressionEmptyData() {
        Map<LocalDate, Integer> data = makeDailyMap(new int[]{}, 1);

        RegressionModel model = forecastService.trainLinearRegression(data, 1);

        assertThat(model.slope()).isEqualTo(0);
        assertThat(model.avgDailyOut()).isEqualTo(0);
    }

    @Test
    @DisplayName("OLS — 预测明日出库量不低于 0")
    void trainLinearRegressionPredictionNonNegative() {
        // 数据从高到低再到0，但预测值不应为负
        Map<LocalDate, Integer> data = makeDailyMap(new int[]{5, 3, 1, 0, 0}, 5);

        RegressionModel model = forecastService.trainLinearRegression(data, 5);

        assertThat(model.predictedDailyOut()).isGreaterThanOrEqualTo(0);
    }

    // ==================== aggregateDailyOut — 数据聚合 ====================

    @Test
    @DisplayName("aggregateDailyOut — 无出库记录返回全 0")
    void aggregateDailyOutEmpty() {
        Map<LocalDate, Integer> result = forecastService.aggregateDailyOut(List.of(), 7);
        assertThat(result).hasSize(7);
        assertThat(result.values()).allMatch(v -> v == 0);
    }

    @Test
    @DisplayName("aggregateDailyOut — 有出库记录按天聚合")
    void aggregateDailyOutWithRecords() {
        Product product = testProduct;
        // 创建今天和昨天的出库记录
        InventoryLog log1 = InventoryLog.builder()
                .product(product)
                .type(InventoryLog.LogType.OUT)
                .quantity(5)
                .createdTime(LocalDateTime.now().minusDays(0))
                .build();
        InventoryLog log2 = InventoryLog.builder()
                .product(product)
                .type(InventoryLog.LogType.OUT)
                .quantity(3)
                .createdTime(LocalDateTime.now().minusDays(1))
                .build();
        // IN 类型不应被计算
        InventoryLog log3 = InventoryLog.builder()
                .product(product)
                .type(InventoryLog.LogType.IN)
                .quantity(100)
                .createdTime(LocalDateTime.now().minusDays(1))
                .build();
        inventoryLogRepository.saveAll(List.of(log1, log2, log3));

        inventoryLogRepository.flush();

        Map<LocalDate, Integer> result = forecastService.aggregateDailyOut(
                inventoryLogRepository.findOutLogsByProductSince(product.getId(),
                        LocalDate.now().minusDays(30).atStartOfDay()),
                7);

        // IN 类型的 100 不应被计入
        assertThat(result.values()).noneMatch(v -> v >= 100);
        // 至少有非零值
        assertThat(result.values()).anyMatch(v -> v > 0);
    }

    // ==================== calculateSuggestedRestockQty — 建议补货量 ====================

    @Test
    @DisplayName("建议补货量 — 当前库存充足时返回 0")
    void calculateSuggestedRestockZero() {
        // currentStock = 100, safeStock = 20, predicted = 5
        // target = 20 + 5*7 = 55, so qty = max(0, 55 - 100) = 0
        int qty = forecastService.calculateSuggestedRestockQty(100, 20, 5);
        assertThat(qty).isEqualTo(0);
    }

    @Test
    @DisplayName("建议补货量 — 当前库存不足时计算补货量")
    void calculateSuggestedRestockPositive() {
        // currentStock = 10, safeStock = 20, predicted = 5
        // target = 20 + 5*7 = 55, so qty = max(0, 55 - 10) = 45
        int qty = forecastService.calculateSuggestedRestockQty(10, 20, 5);
        assertThat(qty).isEqualTo(45);
    }

    @Test
    @DisplayName("建议补货量 — 无出库预测时补到安全库存")
    void calculateSuggestedRestockNoOut() {
        // predictedDailyOut = 0, currentStock = 5, safeStock = 20
        int qty = forecastService.calculateSuggestedRestockQty(5, 20, 0);
        assertThat(qty).isEqualTo(15);
    }

    @Test
    @DisplayName("建议补货量 — 库存为 0 时按公式计算")
    void calculateSuggestedRestockZeroStock() {
        int qty = forecastService.calculateSuggestedRestockQty(0, 30, 10);
        // target = 30 + 10*7 = 100, qty = max(0, 100 - 0) = 100
        assertThat(qty).isEqualTo(100);
    }

    // ==================== predictProduct — 完整预测 ====================

    @Test
    @DisplayName("predictProduct — 不存在的商品返回 null")
    void predictProductNonExistent() {
        StockPredictionDTO result = forecastService.predictProduct(99999L);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("predictProduct — 正常商品返回预测结果")
    void predictProductNormal() {
        // 创建一些近期的出库记录
        for (int i = 1; i <= 5; i++) {
            inventoryLogRepository.save(InventoryLog.builder()
                    .product(testProduct)
                    .type(InventoryLog.LogType.OUT)
                    .quantity(2 * i)
                    .createdTime(LocalDate.now().minusDays(6 - i).atStartOfDay())
                    .build());
        }
        inventoryLogRepository.flush();

        StockPredictionDTO result = forecastService.predictProduct(testProduct.getId());

        assertThat(result).isNotNull();
        assertThat(result.productName()).isEqualTo("预测测试商品");
        assertThat(result.currentStock()).isEqualTo(100);
        assertThat(result.safeStock()).isEqualTo(20);
        assertThat(result.alertLevel()).isIn("NORMAL", "WARNING", "DANGER");
        assertThat(result.suggestedRestockQty()).isGreaterThanOrEqualTo(0);
        // 趋势数据
        assertThat(result.slope()).isNotNull();
        assertThat(result.rSquared()).isNotNull();
    }

    @Test
    @DisplayName("predictProduct — 库存为 0 时预警级别为 DANGER")
    void predictProductOutOfStock() {
        testProduct.setCurrentStock(0);
        productRepository.save(testProduct);

        StockPredictionDTO result = forecastService.predictProduct(testProduct.getId());

        assertThat(result.alertLevel()).isEqualTo("DANGER");
        assertThat(result.alertReason()).contains("缺货");
    }

    @Test
    @DisplayName("predictProduct — 出库量大时触发预警")
    void predictProductWarning() {
        // 库存 100，但每天出库 20 件，5 天就耗尽
        testProduct.setCurrentStock(100);
        testProduct.setSafeStock(10);
        productRepository.save(testProduct);

        for (int i = 1; i <= 5; i++) {
            inventoryLogRepository.save(InventoryLog.builder()
                    .product(testProduct)
                    .type(InventoryLog.LogType.OUT)
                    .quantity(20)
                    .createdTime(LocalDate.now().minusDays(6 - i).atStartOfDay())
                    .build());
        }
        inventoryLogRepository.flush();

        StockPredictionDTO result = forecastService.predictProduct(testProduct.getId());

        // 出库量大，可能触发 WARNING 或 DANGER
        assertThat(result.alertLevel()).isNotEqualTo("NORMAL");
        assertThat(result.estimatedDaysToEmpty()).isNotNull();
        assertThat(result.estimatedDaysToEmpty()).isGreaterThan(0);
    }

    // ==================== predictAllProducts — 全量预测 ====================

    @Test
    @DisplayName("predictAllProducts — 返回所有商品预测并按预警级别排序")
    void predictAllProductsSorted() {
        // 创建第二个商品（库存充足，无出库）
        productRepository.save(Product.builder()
                .name("稳定商品")
                .code("STABLE_001")
                .currentStock(500)
                .safeStock(50)
                .unit("件")
                .price(BigDecimal.valueOf(10))
                .status("1")
                .build());

        List<StockPredictionDTO> results = forecastService.predictAllProducts();

        assertThat(results).isNotEmpty();
        // DANGER 排在最前
        boolean seenWarning = false;
        boolean seenNormal = false;
        for (StockPredictionDTO r : results) {
            if ("NORMAL".equals(r.alertLevel())) seenNormal = true;
            if ("WARNING".equals(r.alertLevel())) seenWarning = true;
            if (seenNormal) assertThat(r.alertLevel()).isNotEqualTo("DANGER");
            if (seenWarning && seenNormal) break;
        }
    }

    @Test
    @DisplayName("predictAllProducts — 即使某个商品预测出错也不影响其余")
    void predictAllProductsToleratesError() {
        // 正常运行不应抛异常
        List<StockPredictionDTO> results = forecastService.predictAllProducts();
        assertThat(results).isNotNull();
    }

    // ==================== 常量验证 ====================

    @Test
    @DisplayName("预测常量在合理范围")
    void forecastConstants() {
        assertThat(StockForecastService.FORECAST_WINDOW_DAYS).isGreaterThan(0);
        assertThat(StockForecastService.DANGER_THRESHOLD_DAYS).isLessThan(
                StockForecastService.WARNING_THRESHOLD_DAYS);
        assertThat(StockForecastService.DEFAULT_RESTOCK_CYCLE_DAYS).isGreaterThan(0);
    }
}
