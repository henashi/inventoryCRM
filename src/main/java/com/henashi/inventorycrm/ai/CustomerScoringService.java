package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.CustomerScoreDTO;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.GiftLog;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.GiftLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户评分算法服务
 * <p>
 * 六维加权评分模型，纯 Java 实现：
 * <ol>
 *   <li>客户等级 giftLevel (30%)</li>
 *   <li>活跃度 recency (20%)</li>
 *   <li>注册时长 tenure (15%)</li>
 *   <li>领取频率 frequency (15%)</li>
 *   <li>推荐贡献 referral (10%)</li>
 *   <li>生日临近 birthday (10%)</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerScoringService {

    /** 各维度权重 */
    private static final double WEIGHT_GIFT_LEVEL = 0.30;
    private static final double WEIGHT_RECENCY = 0.20;
    private static final double WEIGHT_TENURE = 0.15;
    private static final double WEIGHT_FREQUENCY = 0.15;
    private static final double WEIGHT_REFERRAL = 0.10;
    private static final double WEIGHT_BIRTHDAY = 0.10;

    /** 分段阈值 */
    private static final double HIGH_VALUE_THRESHOLD = 80;
    private static final double GROWING_THRESHOLD = 60;

    private final CustomerRepository customerRepository;
    private final GiftLogRepository giftLogRepository;

    /**
     * 计算单个客户的评分
     */
    public CustomerScoreDTO scoreCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) return null;
        return buildScore(customer);
    }

    /**
     * 计算所有客户的评分（按总分降序）
     */
    public List<CustomerScoreDTO> scoreAllCustomers() {
        List<Customer> allCustomers = customerRepository.findAll();
        List<CustomerScoreDTO> results = new ArrayList<>();

        for (Customer customer : allCustomers) {
            try {
                CustomerScoreDTO score = buildScore(customer);
                if (score != null) results.add(score);
            } catch (Exception e) {
                log.warn("评分客户 {} (ID={}) 时出错: {}", customer.getName(), customer.getId(), e.getMessage());
            }
        }

        results.sort((a, b) -> Double.compare(b.totalScore(), a.totalScore()));
        return results;
    }

    /**
     * 获取未来 7 天生日的客户
     */
    public List<Customer> findUpcomingBirthdayCustomers() {
        LocalDate today = LocalDate.now();
        return customerRepository.findAll().stream()
                .filter(c -> c.getBirthday() != null)
                .filter(c -> {
                    LocalDate nextBirthday = c.getBirthday().withYear(today.getYear());
                    if (nextBirthday.isBefore(today)) {
                        nextBirthday = nextBirthday.plusYears(1);
                    }
                    long days = ChronoUnit.DAYS.between(today, nextBirthday);
                    return days >= 0 && days <= 7;
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建单个客户评分
     */
    private CustomerScoreDTO buildScore(Customer customer) {
        Map<String, Double> dimScores = new LinkedHashMap<>();

        // 1. 客户等级 (30%)
        double giftLevelScore = scoreGiftLevel(customer.getGiftLevel());
        dimScores.put("giftLevel", giftLevelScore);

        // 2. 活跃度 (20%)
        double recencyScore = scoreRecency(customer);
        dimScores.put("recency", recencyScore);

        // 3. 注册时长 (15%)
        double tenureScore = scoreTenure(customer);
        dimScores.put("tenure", tenureScore);

        // 4. 领取频率 (15%)
        double frequencyScore = scoreFrequency(customer);
        dimScores.put("frequency", frequencyScore);

        // 5. 推荐贡献 (10%)
        double referralScore = scoreReferral(customer);
        dimScores.put("referral", referralScore);

        // 6. 生日临近 (10%)
        double birthdayScore = scoreBirthday(customer);
        dimScores.put("birthday", birthdayScore);

        // 加权总分
        double totalScore = giftLevelScore * WEIGHT_GIFT_LEVEL
                + recencyScore * WEIGHT_RECENCY
                + tenureScore * WEIGHT_TENURE
                + frequencyScore * WEIGHT_FREQUENCY
                + referralScore * WEIGHT_REFERRAL
                + birthdayScore * WEIGHT_BIRTHDAY;

        totalScore = Math.round(totalScore * 100.0) / 100.0;

        // 分段
        String segment;
        if (totalScore >= HIGH_VALUE_THRESHOLD) segment = "HIGH_VALUE";
        else if (totalScore >= GROWING_THRESHOLD) segment = "GROWING";
        else segment = "INACTIVE";

        // 生日信息
        boolean isBirthdaySoon = false;
        Integer daysToBirthday = null;
        if (customer.getBirthday() != null) {
            LocalDate today = LocalDate.now();
            LocalDate next = customer.getBirthday().withYear(today.getYear());
            if (next.isBefore(today)) next = next.plusYears(1);
            daysToBirthday = (int) ChronoUnit.DAYS.between(today, next);
            isBirthdaySoon = daysToBirthday >= 0 && daysToBirthday <= 7;
        }

        return new CustomerScoreDTO(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getGiftLevel(),
                totalScore,
                segment,
                dimScores,
                isBirthdaySoon,
                customer.getBirthday() != null ? customer.getBirthday().toString() : null,
                daysToBirthday,
                customer.getStatus()
        );
    }

    // ==================== 六维评分方法 ====================

    /**
     * 1. 客户等级评分 (0-100)
     */
    double scoreGiftLevel(Integer giftLevel) {
        if (giftLevel == null || giftLevel <= 0) return 0;
        return switch (giftLevel) {
            case 1 -> 60;
            case 2 -> 80;
            case 3 -> 100;
            default -> Math.min(giftLevel * 30, 100);
        };
    }

    /**
     * 2. 活跃度评分 (0-100)
     * 最近领取距今天数，≤7天→100分，每+7天减20分
     */
    double scoreRecency(Customer customer) {
        if (customer.getGiftReceivedAt() == null) return 0;
        long daysSinceLastGift = ChronoUnit.DAYS.between(customer.getGiftReceivedAt().toLocalDate(), LocalDate.now());
        if (daysSinceLastGift <= 7) return 100;
        return Math.max(0, 100 - (daysSinceLastGift / 7) * 20);
    }

    /**
     * 3. 注册时长评分 (0-100)
     * ≥1年→100分，每少1月减8分
     */
    double scoreTenure(Customer customer) {
        if (customer.getRegisteredAt() == null) return 0;
        long months = ChronoUnit.MONTHS.between(customer.getRegisteredAt(), LocalDate.now());
        if (months >= 12) return 100;
        return Math.max(0, 100 - (12 - months) * 8);
    }

    /**
     * 4. 领取频率评分 (0-100)
     * 月均领取次数，≥3次→100分，线性递减
     */
    double scoreFrequency(Customer customer) {
        List<GiftLog> logs = giftLogRepository.findByCustomerId(customer.getId(),
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        if (logs.isEmpty()) return 0;

        LocalDate registered = customer.getRegisteredAt() != null ? customer.getRegisteredAt() : LocalDate.now();
        long months = Math.max(1, ChronoUnit.MONTHS.between(registered, LocalDate.now()));
        double avgPerMonth = (double) logs.size() / months;

        return Math.min(avgPerMonth / 3.0 * 100, 100);
    }

    /**
     * 5. 推荐贡献评分 (0-100)
     * 有推荐人→50分，被推荐人数≥3→100分
     */
    double scoreReferral(Customer customer) {
        int referralCount = customer.getReferrals() != null ? customer.getReferrals().size() : 0;
        if (customer.hasReferrer()) {
            return 50 + Math.min(referralCount * 15, 50);
        }
        return Math.min(referralCount * 30, 100);
    }

    /**
     * 6. 生日临近评分 (0-100)
     * 未来7天内生日→100分，每远1天减10分
     */
    double scoreBirthday(Customer customer) {
        if (customer.getBirthday() == null) return 0;
        LocalDate today = LocalDate.now();
        LocalDate nextBirthday = customer.getBirthday().withYear(today.getYear());
        if (nextBirthday.isBefore(today)) nextBirthday = nextBirthday.plusYears(1);

        long days = ChronoUnit.DAYS.between(today, nextBirthday);
        if (days <= 7) return 100;
        if (days <= 30) return Math.max(0, 100 - (days - 7) * 4);
        return 0;
    }
}
