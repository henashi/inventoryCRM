package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.StockPredictionDTO;
import com.henashi.inventorycrm.ai.dto.StockPredictionDTO.DailyOutRecord;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.InventoryLogRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存预测算法服务
 * <p>
 * 使用 OLS（普通最小二乘法）线性回归预测库存趋势。
 * 纯 Java 实现，零外部 ML 依赖。
 * 包含：模型训练 → 趋势推断 → 置信度评估 → 策略建议。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockForecastService {

    /** 预测窗口：分析最近 N 天的出库数据 */
    public static final int FORECAST_WINDOW_DAYS = 30;

    /** 短期窗口 */
    public static final int SHORT_WINDOW_DAYS = 7;

    /** 高危阈值：预计耗尽天数 ≤ 此值 */
    public static final int DANGER_THRESHOLD_DAYS = 7;

    /** 预警阈值：预计耗尽天数 ≤ 此值 */
    public static final int WARNING_THRESHOLD_DAYS = 14;

    /** 默认补货周期（天） */
    public static final int DEFAULT_RESTOCK_CYCLE_DAYS = 7;

    /** R² 置信度阈值：低于此值视为低可信度 */
    private static final double LOW_CONFIDENCE_THRESHOLD = 0.3;

    private final ProductRepository productRepository;
    private final InventoryLogRepository inventoryLogRepository;

    /**
     * OLS 线性回归模型训练结果
     */
    public record RegressionModel(
            double slope,          // 斜率（每日出库量变化趋势）
            double intercept,      // 截距
            double rSquared,       // 决定系数 R²（0~1，模型拟合优度）
            double predictedDailyOut,  // 预测明日出库量
            double avgDailyOut,    // 历史日均出库量（用于对比）
            String trendDirection  // UP / DOWN / STABLE
    ) {}

    public StockPredictionDTO predictProduct(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            log.warn("商品不存在: productId={}", productId);
            return null;
        }
        return buildPrediction(product);
    }

    /**
     * 对所有商品执行全量预测
     */
    public List<StockPredictionDTO> predictAllProducts() {
        List<Product> allProducts = productRepository.findAll();
        List<StockPredictionDTO> results = new ArrayList<>();

        for (Product product : allProducts) {
            try {
                StockPredictionDTO prediction = buildPrediction(product);
                if (prediction != null) {
                    results.add(prediction);
                }
            } catch (Exception e) {
                log.warn("预测商品 {} (ID={}) 时出错: {}", product.getName(), product.getId(), e.getMessage());
            }
        }

        // 按预警级别排序：DANGER > WARNING > NORMAL
        results.sort((a, b) -> {
            int cmp = severityOrder(a.alertLevel()) - severityOrder(b.alertLevel());
            if (cmp != 0) return cmp;
            int daysA = a.estimatedDaysToEmpty() != null && a.estimatedDaysToEmpty() >= 0
                    ? a.estimatedDaysToEmpty() : Integer.MAX_VALUE;
            int daysB = b.estimatedDaysToEmpty() != null && b.estimatedDaysToEmpty() >= 0
                    ? b.estimatedDaysToEmpty() : Integer.MAX_VALUE;
            return Integer.compare(daysA, daysB);
        });

        return results;
    }

    /**
     * 构建单个商品预测
     */
    private StockPredictionDTO buildPrediction(Product product) {
        int currentStock = defaultStock(product.getCurrentStock());
        int safeStock = defaultStock(product.getSafeStock());

        // === 数据采集：获取近 FORECAST_WINDOW_DAYS 天的出库记录 ===
        LocalDateTime since = LocalDate.now().minusDays(FORECAST_WINDOW_DAYS).atStartOfDay();
        List<InventoryLog> outLogs = inventoryLogRepository.findOutLogsByProductSince(product.getId(), since);

        // === 按天聚合出库量 ===
        Map<LocalDate, Integer> dailyOutMap = aggregateDailyOut(outLogs, FORECAST_WINDOW_DAYS);

        // 生成每日出库记录（前端趋势图用）
        List<DailyOutRecord> dailyRecords = dailyOutMap.entrySet().stream()
                .map(e -> new DailyOutRecord(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(DailyOutRecord::date))
                .collect(Collectors.toList());

        // === 训练 OLS 线性回归模型 ===
        RegressionModel model = trainLinearRegression(dailyOutMap, FORECAST_WINDOW_DAYS);

        // === 模型推理：预测近7日 / 30日平均出库量 ===
        BigDecimal avgDailyOut7d = BigDecimal.valueOf(model.predictedDailyOut())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal avgDailyOut30d = BigDecimal.valueOf(model.avgDailyOut())
                .setScale(2, RoundingMode.HALF_UP);

        // === 预测耗尽天数 ===
        int estimatedDaysToEmpty = -1;
        int estimatedDaysToSafe = -1;
        double predictedRate = model.predictedDailyOut();
        if (predictedRate > 0) {
            estimatedDaysToEmpty = (int) Math.floor(currentStock / predictedRate);
            estimatedDaysToSafe = safeStock > 0
                    ? (int) Math.floor((currentStock - safeStock) / predictedRate)
                    : estimatedDaysToEmpty;
        }

        // === 计算建议补货量 ===
        int suggestedRestockQty = calculateSuggestedRestockQty(currentStock, safeStock, predictedRate);

        // === 决策：预警级别 + 原因 ===
        String alertLevel = "NORMAL";
        String alertReason = null;

        if (currentStock <= 0) {
            alertLevel = "DANGER";
            alertReason = "当前库存为0，已缺货";
        } else if (estimatedDaysToEmpty >= 0 && estimatedDaysToEmpty <= DANGER_THRESHOLD_DAYS) {
            alertLevel = "DANGER";
            alertReason = String.format("线性回归预测 %d 天后耗尽（≤%d天），趋势%s，建议立即补货",
                    estimatedDaysToEmpty, DANGER_THRESHOLD_DAYS,
                    formatTrend(model.trendDirection()));
        } else if (estimatedDaysToEmpty >= 0 && estimatedDaysToEmpty <= WARNING_THRESHOLD_DAYS) {
            alertLevel = "WARNING";
            alertReason = String.format("线性回归预测 %d 天后耗尽（≤%d天），趋势%s，请关注",
                    estimatedDaysToEmpty, WARNING_THRESHOLD_DAYS,
                    formatTrend(model.trendDirection()));
        }

        // 低置信度时补充说明
        if (model.rSquared() >= 0 && model.rSquared() < LOW_CONFIDENCE_THRESHOLD
                && alertReason != null && !alertLevel.equals("NORMAL")) {
            alertReason += "（数据波动大，预测置信度偏低）";
        }

        return new StockPredictionDTO(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getCategory(),
                product.getUnit(),
                currentStock,
                safeStock,
                avgDailyOut7d,
                avgDailyOut30d,
                estimatedDaysToEmpty,
                estimatedDaysToSafe,
                suggestedRestockQty,
                alertLevel,
                alertReason,
                BigDecimal.valueOf(model.slope()).setScale(4, RoundingMode.HALF_UP).doubleValue(),
                BigDecimal.valueOf(model.rSquared()).setScale(4, RoundingMode.HALF_UP).doubleValue(),
                model.trendDirection(),
                dailyRecords
        );
    }

    // ============================================================
    //  OLS 线性回归 — 核心算法
    // ============================================================

    /**
     * 训练 OLS 线性回归模型。
     * <p>
     * 输入：每日出库量映射表
     * 输出：slope（斜率）、intercept（截距）、R²（拟合优度）、predictedDailyOut（预测值）
     * <p>
     * 公式：
     *   slope = Σ((xi - x̄)(yi - ȳ)) / Σ(xi - x̄)²
     *   intercept = ȳ - slope × x̄
     *   R² = 1 - SSres / SStot
     */
    RegressionModel trainLinearRegression(Map<LocalDate, Integer> dailyMap, int windowDays) {
        // 提取最近 windowDays 天的数据点 (x=天索引, y=出库量)
        List<Map.Entry<LocalDate, Integer>> entries = dailyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        int start = Math.max(0, entries.size() - windowDays);
        List<Map.Entry<LocalDate, Integer>> window = entries.subList(start, entries.size());

        int n = window.size();
        if (n < 3) {
            // 数据点太少，无法训练回归，返回简单平均值
            double avg = n > 0
                    ? window.stream().mapToInt(Map.Entry::getValue).average().orElse(0)
                    : 0;
            return new RegressionModel(0, avg, 0, avg, avg, "STABLE");
        }

        // 计算 x̄ 和 ȳ
        double sumX = 0, sumY = 0;
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i;  // 以索引为 X（0,1,2,...），消除日期间隔不一致的影响
            y[i] = window.get(i).getValue();
            sumX += x[i];
            sumY += y[i];
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        // 计算 slope = Σ((xi - x̄)(yi - ȳ)) / Σ(xi - x̄)²
        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < n; i++) {
            numerator += (x[i] - meanX) * (y[i] - meanY);
            denominator += (x[i] - meanX) * (x[i] - meanX);
        }

        double slope;
        double intercept;
        if (denominator == 0) {
            // 所有 X 值相同（理论上不会发生，因为 x[i] = i 是递增的）
            slope = 0;
            intercept = meanY;
        } else {
            slope = numerator / denominator;
            intercept = meanY - slope * meanX;
        }

        // 计算 R² = 1 - SSres / SStot
        double ssRes = 0;  // 残差平方和
        double ssTot = 0;  // 总平方和
        for (int i = 0; i < n; i++) {
            double yPred = slope * x[i] + intercept;
            ssRes += (y[i] - yPred) * (y[i] - yPred);
            ssTot += (y[i] - meanY) * (y[i] - meanY);
        }
        double rSquared = ssTot > 0 ? 1 - ssRes / ssTot : 0;

        // 预测明天的出库量（dayIndex = n）
        double predictedDailyOut = slope * n + intercept;
        if (predictedDailyOut < 0) predictedDailyOut = 0;

        // 历史日均出库量
        double avgDailyOut = sumY / n;

        // 趋势方向判断
        String trendDirection = "STABLE";
        if (Math.abs(slope) > 0.05) {  // 斜率超过阈值才视为有趋势
            trendDirection = slope > 0 ? "UP" : "DOWN";
        }

        return new RegressionModel(
                BigDecimal.valueOf(slope).setScale(4, RoundingMode.HALF_UP).doubleValue(),
                BigDecimal.valueOf(intercept).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                BigDecimal.valueOf(rSquared).setScale(4, RoundingMode.HALF_UP).doubleValue(),
                BigDecimal.valueOf(predictedDailyOut).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                BigDecimal.valueOf(avgDailyOut).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                trendDirection
        );
    }

    // ============================================================
    //  数据预处理
    // ============================================================

    /**
     * 按天聚合出库量，缺失天补 0
     */
    Map<LocalDate, Integer> aggregateDailyOut(List<InventoryLog> outLogs, int windowDays) {
        Map<LocalDate, Integer> dailyMap = new LinkedHashMap<>();

        LocalDate today = LocalDate.now();
        for (int i = windowDays - 1; i >= 0; i--) {
            dailyMap.put(today.minusDays(i), 0);
        }

        for (InventoryLog log : outLogs) {
            if (log.getType() != InventoryLog.LogType.OUT) continue;
            LocalDate logDate = log.getCreatedTime().toLocalDate();
            dailyMap.merge(logDate, Math.abs(defaultStock(log.getQuantity())), Integer::sum);
        }

        return dailyMap;
    }

    // ============================================================
    //  决策逻辑
    // ============================================================

    /**
     * 计算建议补货量
     * <p>
     * 建议补货量 = max(0, safeStock + 预测日出库量 × 补货周期 - 当前库存)
     */
    int calculateSuggestedRestockQty(int currentStock, int safeStock, double predictedDailyOut) {
        if (predictedDailyOut <= 0) {
            return Math.max(0, safeStock - currentStock);
        }
        double target = safeStock + predictedDailyOut * DEFAULT_RESTOCK_CYCLE_DAYS;
        int qty = (int) Math.ceil(target - currentStock);
        return Math.max(0, qty);
    }

    /**
     * 趋势方向的中文描述
     */
    private String formatTrend(String direction) {
        return switch (direction) {
            case "UP" -> "↑ 上升";
            case "DOWN" -> "↓ 下降";
            default -> "→ 平稳";
        };
    }

    private int severityOrder(String level) {
        if (level == null) return 3;
        return switch (level) {
            case "DANGER" -> 0;
            case "WARNING" -> 1;
            default -> 2;
        };
    }

    private int defaultStock(Integer stock) {
        return stock == null ? 0 : stock;
    }
}
