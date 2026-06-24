package com.henashi.inventorycrm.ai.controller;

import com.henashi.inventorycrm.ai.CustomerScoringAgent;
import com.henashi.inventorycrm.ai.GiftRecommendationService;
import com.henashi.inventorycrm.ai.StockPredictionAgent;
import com.henashi.inventorycrm.ai.dto.CustomerScoreDTO;
import com.henashi.inventorycrm.ai.dto.GiftRecommendationDTO;
import com.henashi.inventorycrm.ai.dto.PredictionSummaryDTO;
import com.henashi.inventorycrm.ai.dto.StockAlertDTO;
import com.henashi.inventorycrm.ai.dto.StockPredictionDTO;
import com.henashi.inventorycrm.pojo.Customer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Agent 统一控制器
 * <p>
 * 提供库存预测、预警、分析等 AI 能力的 API 入口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
@Tag(name = "AI Agent 控制器", description = "AI 库存预测 Agent 的统一 API 入口")
public class AiAgentController {

    private final StockPredictionAgent predictionAgent;
    private final CustomerScoringAgent scoringAgent;
    private final GiftRecommendationService recommendationService;

    @GetMapping("/predictions")
    @Operation(summary = "查询所有商品库存预测（分页）", description = "返回所有商品的预测结果，按预警级别排序，支持关键词搜索")
    public Page<StockPredictionDTO> getPredictions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return predictionAgent.getPredictions(page, size, keyword);
    }

    @GetMapping("/predictions/{productId}")
    @Operation(summary = "查询单个商品预测详情", description = "返回单个商品的预测详情，含每日出库记录趋势数据")
    public ResponseEntity<StockPredictionDTO> getPredictionByProduct(
            @PathVariable("productId") Long productId
    ) {
        StockPredictionDTO result = predictionAgent.getPredictionByProductId(productId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/predictions/summary")
    @Operation(summary = "查询预测概览统计", description = "返回全部商品的预测统计摘要（高危数/预警数/补货总量等）")
    public PredictionSummaryDTO getSummary() {
        return predictionAgent.getSummary();
    }

    @GetMapping("/alerts")
    @Operation(summary = "查询高危/预警商品列表", description = "按预警级别过滤，不传 level 返回全部")
    public List<StockAlertDTO> getAlerts(
            @RequestParam(name = "level", required = false, defaultValue = "ALL") String level
    ) {
        return predictionAgent.getAlerts(level);
    }

    @PostMapping("/predictions/run")
    @Operation(summary = "手动触发全量预测", description = "立即执行一次全量预测并刷新缓存")
    public ResponseEntity<Map<String, Object>> runPrediction() {
        long startMs = System.currentTimeMillis();
        long elapsed = predictionAgent.runPrediction();
        PredictionSummaryDTO summary = predictionAgent.getSummary();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("executionTimeMs", elapsed);
        result.put("totalPredicted", summary.totalPredicted());
        result.put("dangerCount", summary.dangerCount());
        result.put("warningCount", summary.warningCount());
        return ResponseEntity.ok(result);
    }

    // ==================== 客户评分 & 礼品推荐 ====================

    @GetMapping("/customers/scores")
    @Operation(summary = "查询客户评分列表（分页）", description = "返回所有客户六维评分结果，按总分降序")
    public Page<CustomerScoreDTO> getCustomerScores(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return scoringAgent.getScores(page, size);
    }

    @GetMapping("/customers/{customerId}/score")
    @Operation(summary = "查询单个客户评分详情", description = "返回单个客户的六维评分明细（雷达图数据）")
    public ResponseEntity<CustomerScoreDTO> getCustomerScore(
            @PathVariable("customerId") Long customerId
    ) {
        CustomerScoreDTO result = scoringAgent.getScoreByCustomerId(customerId);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/customers/{customerId}/recommendations")
    @Operation(summary = "查询客户礼品推荐", description = "返回 Top-3 推荐礼品及匹配理由")
    public List<GiftRecommendationDTO> getRecommendations(
            @PathVariable("customerId") Long customerId
    ) {
        return recommendationService.recommendForCustomer(customerId);
    }

    @GetMapping("/customers/birthday-upcoming")
    @Operation(summary = "查询未来7天生日的客户", description = "返回即将生日的客户列表，用于生日提醒和自动发放")
    public List<Customer> getUpcomingBirthdayCustomers() {
        return scoringAgent.getUpcomingBirthdayCustomers();
    }

    @PostMapping("/customers/run-scoring")
    @Operation(summary = "手动触发全量客户评分", description = "立即执行一次全量评分并刷新缓存")
    public ResponseEntity<Map<String, Object>> runScoring() {
        long elapsed = scoringAgent.runScoring();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("executionTimeMs", elapsed);
        return ResponseEntity.ok(result);
    }
}
