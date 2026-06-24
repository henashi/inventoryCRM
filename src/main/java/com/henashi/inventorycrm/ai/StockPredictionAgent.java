package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.PredictionSummaryDTO;
import com.henashi.inventorycrm.ai.dto.StockAlertDTO;
import com.henashi.inventorycrm.ai.dto.StockPredictionDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * AI 库存预测 Agent
 * <p>
 * 感知 → 思考 → 行动 的 Agent 闭环：
 * <ol>
 *   <li><b>感知</b>：从 inventory_log 读取历史出库数据</li>
 *   <li><b>思考</b>：StockForecastService 执行加权移动平均预测</li>
 *   <li><b>记忆</b>：预测结果缓存到 Caffeine（5分钟过期，避免重复计算）</li>
 *   <li><b>行动</b>：返回预警信息 / 补货建议 / 趋势数据，供前端展示</li>
 * </ol>
 * <p>
 * 每天凌晨 1:00 自动全量执行一次。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockPredictionAgent {

    private final StockForecastService forecastService;
    private final CacheManager cacheManager;

    /** 缓存名称（与 CacheConfig 保持一致） */
    public static final String PREDICTION_CACHE_NAME = "stockPredictions";

    /** 缓存 Key */
    private static final String ALL_PREDICTIONS_KEY = "all";
    private static final String SUMMARY_KEY = "summary";
    private static final String ALERTS_KEY = "alerts";

    /** 上次执行时间 */
    private final AtomicReference<LocalDateTime> lastRunTime = new AtomicReference<>(null);

    @PostConstruct
    public void init() {
        log.info("StockPredictionAgent 已初始化，每天凌晨 1:00 自动执行全量预测");
        log.info("预测参数: 窗口={}天, 高危阈值≤{}天, 预警阈值≤{}天",
                StockForecastService.FORECAST_WINDOW_DAYS,
                StockForecastService.DANGER_THRESHOLD_DAYS,
                StockForecastService.WARNING_THRESHOLD_DAYS);
    }

    /**
     * 定时任务：每天凌晨 1:00 自动执行全量预测
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledPrediction() {
        log.info("=== [定时触发] StockPredictionAgent 开始全量预测 ===");
        long startMs = System.currentTimeMillis();
        executeInternal();
        long elapsed = System.currentTimeMillis() - startMs;
        log.info("=== [定时触发] 全量预测完成，耗时 {}ms ===", elapsed);
    }

    /**
     * 手动触发全量预测（供 Controller/API 调用）
     *
     * @return 执行耗时（毫秒）
     */
    public long runPrediction() {
        log.info("=== [手动触发] StockPredictionAgent 开始全量预测 ===");
        long startMs = System.currentTimeMillis();
        executeInternal();
        long elapsed = System.currentTimeMillis() - startMs;
        log.info("=== [手动触发] 全量预测完成，耗时 {}ms ===", elapsed);
        return elapsed;
    }

    /**
     * Agent 核心执行逻辑
     */
    private void executeInternal() {
        // === 感知：获取所有商品预测 ===
        List<StockPredictionDTO> predictions = forecastService.predictAllProducts();

        // === 记忆：写入缓存 ===
        Cache cache = cacheManager.getCache(PREDICTION_CACHE_NAME);
        if (cache != null) {
            cache.put(ALL_PREDICTIONS_KEY, predictions);
            cache.put(SUMMARY_KEY, buildSummary(predictions));
            cache.put(ALERTS_KEY, extractAlerts(predictions));
        }

        lastRunTime.set(LocalDateTime.now());

        // === 记录日志摘要 ===
        long dangerCount = predictions.stream().filter(p -> "DANGER".equals(p.alertLevel())).count();
        long warningCount = predictions.stream().filter(p -> "WARNING".equals(p.alertLevel())).count();
        log.info("预测完成: 共 {} 个商品, 高危 {} 个, 预警 {} 个",
                predictions.size(), dangerCount, warningCount);
    }

    /**
     * 获取所有商品的预测结果（分页）
     */
    @SuppressWarnings("unchecked")
    public Page<StockPredictionDTO> getPredictions(int page, int size, String keyword) {
        List<StockPredictionDTO> predictions = getCachedOrCompute();
        if (predictions.isEmpty()) {
            return Page.empty();
        }

        // 关键词过滤
        List<StockPredictionDTO> filtered = predictions;
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            filtered = predictions.stream()
                    .filter(p -> (p.productName() != null && p.productName().toLowerCase().contains(kw))
                            || (p.productCode() != null && p.productCode().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        // 分页
        int total = filtered.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        if (start >= total) {
            return Page.empty();
        }
        List<StockPredictionDTO> content = filtered.subList(start, end);
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    /**
     * 获取单个商品的预测
     */
    @SuppressWarnings("unchecked")
    public StockPredictionDTO getPredictionByProductId(Long productId) {
        // 先从缓存取
        Cache cache = cacheManager.getCache(PREDICTION_CACHE_NAME);
        if (cache != null) {
            List<StockPredictionDTO> cached = cache.get(ALL_PREDICTIONS_KEY, List.class);
            if (cached != null) {
                for (StockPredictionDTO p : cached) {
                    if (p.productId().equals(productId)) {
                        return p;
                    }
                }
            }
        }
        // 缓存没有，实时计算
        return forecastService.predictProduct(productId);
    }

    /**
     * 获取概览统计
     */
    @SuppressWarnings("unchecked")
    public PredictionSummaryDTO getSummary() {
        Cache cache = cacheManager.getCache(PREDICTION_CACHE_NAME);
        if (cache != null) {
            PredictionSummaryDTO cached = cache.get(SUMMARY_KEY, PredictionSummaryDTO.class);
            if (cached != null) {
                return cached;
            }
        }
        // 缓存没有，重新计算并缓存
        List<StockPredictionDTO> predictions = forecastService.predictAllProducts();
        PredictionSummaryDTO summary = buildSummary(predictions);
        if (cache != null) {
            cache.put(ALL_PREDICTIONS_KEY, predictions);
            cache.put(SUMMARY_KEY, summary);
        }
        return summary;
    }

    /**
     * 获取高危预警列表
     */
    @SuppressWarnings("unchecked")
    public List<StockAlertDTO> getAlerts(String level) {
        Cache cache = cacheManager.getCache(PREDICTION_CACHE_NAME);
        List<StockAlertDTO> alerts;
        if (cache != null) {
            alerts = cache.get(ALERTS_KEY, List.class);
            if (alerts != null) {
                return filterAlertsByLevel(alerts, level);
            }
        }
        // 缓存没有，重新计算
        List<StockPredictionDTO> predictions = forecastService.predictAllProducts();
        alerts = extractAlerts(predictions);
        if (cache != null) {
            cache.put(ALL_PREDICTIONS_KEY, predictions);
            cache.put(ALERTS_KEY, alerts);
        }
        return filterAlertsByLevel(alerts, level);
    }

    /**
     * 获取上次执行时间
     */
    public LocalDateTime getLastRunTime() {
        return lastRunTime.get();
    }

    // ==================== 内部方法 ====================

    @SuppressWarnings("unchecked")
    private List<StockPredictionDTO> getCachedOrCompute() {
        Cache cache = cacheManager.getCache(PREDICTION_CACHE_NAME);
        if (cache != null) {
            List<StockPredictionDTO> cached = cache.get(ALL_PREDICTIONS_KEY, List.class);
            if (cached != null) {
                return cached;
            }
        }
        // 缓存失效，重新计算
        List<StockPredictionDTO> predictions = forecastService.predictAllProducts();
        if (cache != null) {
            cache.put(ALL_PREDICTIONS_KEY, predictions);
            cache.put(SUMMARY_KEY, buildSummary(predictions));
            cache.put(ALERTS_KEY, extractAlerts(predictions));
        }
        lastRunTime.set(LocalDateTime.now());
        return predictions;
    }

    private PredictionSummaryDTO buildSummary(List<StockPredictionDTO> predictions) {
        int dangerCount = (int) predictions.stream().filter(p -> "DANGER".equals(p.alertLevel())).count();
        int warningCount = (int) predictions.stream().filter(p -> "WARNING".equals(p.alertLevel())).count();
        int normalCount = (int) predictions.stream().filter(p -> "NORMAL".equals(p.alertLevel())).count();
        int totalRestock = predictions.stream()
                .mapToInt(p -> p.suggestedRestockQty() != null ? p.suggestedRestockQty() : 0)
                .sum();

        return new PredictionSummaryDTO(
                predictions.size(),
                dangerCount,
                warningCount,
                normalCount,
                totalRestock,
                0L  // executionTime 由控制器填充
        );
    }

    private List<StockAlertDTO> extractAlerts(List<StockPredictionDTO> predictions) {
        return predictions.stream()
                .filter(p -> "DANGER".equals(p.alertLevel()) || "WARNING".equals(p.alertLevel()))
                .map(p -> new StockAlertDTO(
                        p.productId(),
                        p.productCode(),
                        p.productName(),
                        p.currentStock(),
                        p.safeStock(),
                        p.avgDailyOut7d() != null && p.avgDailyOut7d().compareTo(BigDecimal.ZERO) > 0
                                ? p.avgDailyOut7d() : p.avgDailyOut30d(),
                        p.estimatedDaysToEmpty(),
                        p.suggestedRestockQty(),
                        p.alertLevel(),
                        p.alertReason()
                ))
                .collect(Collectors.toList());
    }

    private List<StockAlertDTO> filterAlertsByLevel(List<StockAlertDTO> alerts, String level) {
        if (level == null || level.isBlank() || "ALL".equalsIgnoreCase(level)) {
            return alerts;
        }
        return alerts.stream()
                .filter(a -> level.equalsIgnoreCase(a.alertLevel()))
                .collect(Collectors.toList());
    }
}
