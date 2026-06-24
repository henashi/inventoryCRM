package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.CustomerScoreDTO;
import com.henashi.inventorycrm.ai.dto.GiftRecommendationDTO;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.dto.GiftLogCreateDTO;
import com.henashi.inventorycrm.service.GiftLogService;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 客户评分 Agent
 * <p>
 * 感知 → 思考 → 行动 闭环：
 * <ol>
 *   <li><b>感知</b>：从 customer / gift_log 读取客户数据和礼品记录</li>
 *   <li><b>思考</b>：CustomerScoringService 六维加权评分 + GiftRecommendationService 礼品匹配</li>
 *   <li><b>记忆</b>：评分结果缓存到 Caffeine（5分钟过期）</li>
 *   <li><b>行动</b>：返回评分排行 / 礼品推荐 / 自动触发生日礼品发放</li>
 * </ol>
 * <p>
 * 每天早上 8:00 自动检查生日客户并触发礼品发放。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerScoringAgent {

    private final CustomerScoringService scoringService;
    private final GiftRecommendationService recommendationService;
    private final GiftLogService giftLogService;
    private final CacheManager cacheManager;

    public static final String SCORING_CACHE_NAME = "customerScores";
    private static final String ALL_SCORES_KEY = "all";
    private static final String BIRTHDAY_KEY = "birthday";

    private final AtomicReference<String> lastRunTime = new AtomicReference<>(null);

    @PostConstruct
    public void init() {
        log.info("CustomerScoringAgent 已初始化，每天早上 8:00 自动检查生日客户");
    }

    /**
     * 定时任务：每天早上 8:00 检查生日客户并自动发放礼品
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduledBirthdayCheck() {
        log.info("=== [定时触发] 检查生日客户 ===");
        List<Customer> birthdayCustomers = scoringService.findUpcomingBirthdayCustomers();
        log.info("今日生日客户数: {}", birthdayCustomers.size());

        int issued = 0;
        for (Customer customer : birthdayCustomers) {
            if (customer.getGiftLevel() == null || customer.getGiftLevel() <= 0) continue;
            try {
                List<GiftRecommendationDTO> recs = recommendationService.recommendForCustomer(customer.getId());
                if (!recs.isEmpty()) {
                    GiftRecommendationDTO top = recs.get(0);
                    giftLogService.saveGiftLog(new GiftLogCreateDTO(
                            customer.getId(), top.giftId(), "system",
                            "系统自动：生日礼品", 1, "生日礼品自动发放"));
                    issued++;
                    log.info("已为客户 {} (ID={}) 自动发放生日礼品: {}", customer.getName(), customer.getId(), top.giftName());
                }
            } catch (Exception e) {
                log.warn("为客户 {} (ID={}) 自动发放生日礼品失败: {}", customer.getName(), customer.getId(), e.getMessage());
            }
        }
        log.info("=== 生日礼品自动发放完成: 共 {} 位客户，成功 {} 位 ===", birthdayCustomers.size(), issued);
    }

    /**
     * 手动触发全量评分
     */
    public long runScoring() {
        long start = System.currentTimeMillis();
        List<CustomerScoreDTO> scores = scoringService.scoreAllCustomers();
        List<Customer> birthdays = scoringService.findUpcomingBirthdayCustomers();
        Cache cache = cacheManager.getCache(SCORING_CACHE_NAME);
        if (cache != null) {
            cache.put(ALL_SCORES_KEY, scores);
            cache.put(BIRTHDAY_KEY, birthdays);
        }
        lastRunTime.set(java.time.LocalDateTime.now().toString());
        long elapsed = System.currentTimeMillis() - start;
        log.info("全量评分完成: {} 个客户, 耗时 {}ms", scores.size(), elapsed);
        return elapsed;
    }

    /** 获取评分列表（分页） */
    @SuppressWarnings("unchecked")
    public Page<CustomerScoreDTO> getScores(int page, int size) {
        List<CustomerScoreDTO> scores = getCachedScores();
        if (scores.isEmpty()) return Page.empty();
        int total = scores.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        if (start >= total) return Page.empty();
        return new PageImpl<>(scores.subList(start, end), PageRequest.of(page, size), total);
    }

    /** 获取单个客户评分 */
    @SuppressWarnings("unchecked")
    public CustomerScoreDTO getScoreByCustomerId(Long customerId) {
        Cache cache = cacheManager.getCache(SCORING_CACHE_NAME);
        if (cache != null) {
            List<CustomerScoreDTO> cached = cache.get(ALL_SCORES_KEY, List.class);
            if (cached != null) {
                for (CustomerScoreDTO s : cached) {
                    if (s.customerId().equals(customerId)) return s;
                }
            }
        }
        return scoringService.scoreCustomer(customerId);
    }

    /** 获取生日客户列表 */
    @SuppressWarnings("unchecked")
    public List<Customer> getUpcomingBirthdayCustomers() {
        Cache cache = cacheManager.getCache(SCORING_CACHE_NAME);
        if (cache != null) {
            List<Customer> cached = cache.get(BIRTHDAY_KEY, List.class);
            if (cached != null) return cached;
        }
        return scoringService.findUpcomingBirthdayCustomers();
    }

    @SuppressWarnings("unchecked")
    private List<CustomerScoreDTO> getCachedScores() {
        Cache cache = cacheManager.getCache(SCORING_CACHE_NAME);
        if (cache != null) {
            List<CustomerScoreDTO> cached = cache.get(ALL_SCORES_KEY, List.class);
            if (cached != null) return cached;
        }
        List<CustomerScoreDTO> scores = scoringService.scoreAllCustomers();
        if (cache != null) {
            cache.put(ALL_SCORES_KEY, scores);
            cache.put(BIRTHDAY_KEY, scoringService.findUpcomingBirthdayCustomers());
        }
        return scores;
    }
}
