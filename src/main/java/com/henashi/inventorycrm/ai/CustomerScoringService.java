package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.CustomerScoreDTO;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.OrderItem;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户评分算法服务
 * <p>
 * 五维加权评分模型，基于真实消费数据：
 * <ol>
 *   <li>消费总额 totalSpent (30%) — 订单总金额越高分越高</li>
 *   <li>消费频率 frequency (20%) — 下单越频繁分越高</li>
 *   <li>最近消费 recency (20%) — 距上次下单越近分越高</li>
 *   <li>客户等级 giftLevel (10%) — 礼品等级参考</li>
 *   <li>注册时长 tenure (10%) — 留存时间</li>
 *   <li>推荐贡献 referral (10%) — 推荐人数</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerScoringService {

    private static final double WEIGHT_TOTAL_SPENT = 0.30;
    private static final double WEIGHT_FREQUENCY = 0.20;
    private static final double WEIGHT_RECENCY = 0.20;
    private static final double WEIGHT_GIFT_LEVEL = 0.10;
    private static final double WEIGHT_TENURE = 0.10;
    private static final double WEIGHT_REFERRAL = 0.10;

    private static final double HIGH_VALUE_THRESHOLD = 80;
    private static final double GROWING_THRESHOLD = 60;

    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;

    public CustomerScoreDTO scoreCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) return null;
        return buildScore(customer);
    }

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

    public List<Customer> findUpcomingBirthdayCustomers() {
        LocalDate today = LocalDate.now();
        return customerRepository.findAll().stream()
                .filter(c -> c.getBirthday() != null)
                .filter(c -> {
                    LocalDate nextBirthday = c.getBirthday().withYear(today.getYear());
                    if (nextBirthday.isBefore(today)) nextBirthday = nextBirthday.plusYears(1);
                    return ChronoUnit.DAYS.between(today, nextBirthday) <= 7;
                })
                .collect(Collectors.toList());
    }

    private CustomerScoreDTO buildScore(Customer customer) {
        // 读取消费数据
        List<OrderItem> orders = orderItemRepository.findByCustomerId(customer.getId());
        BigDecimal totalSpent = orders.stream()
                .map(OrderItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int orderCount = orders.size();
        LocalDateTime lastOrderTime = orders.stream()
                .map(OrderItem::getOrderTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        Map<String, Double> dimScores = new LinkedHashMap<>();

        double spentScore = scoreTotalSpent(totalSpent);
        dimScores.put("totalSpent", spentScore);

        double freqScore = scoreFrequency(orderCount);
        dimScores.put("frequency", freqScore);

        double recencyScore = scoreRecency(lastOrderTime);
        dimScores.put("recency", recencyScore);

        double giftLevelScore = scoreGiftLevel(customer.getGiftLevel());
        dimScores.put("giftLevel", giftLevelScore);

        double tenureScore = scoreTenure(customer);
        dimScores.put("tenure", tenureScore);

        double referralScore = scoreReferral(customer);
        dimScores.put("referral", referralScore);

        double totalScore = spentScore * WEIGHT_TOTAL_SPENT
                + freqScore * WEIGHT_FREQUENCY
                + recencyScore * WEIGHT_RECENCY
                + giftLevelScore * WEIGHT_GIFT_LEVEL
                + tenureScore * WEIGHT_TENURE
                + referralScore * WEIGHT_REFERRAL;

        totalScore = Math.round(totalScore * 100.0) / 100.0;

        String segment;
        if (totalScore >= HIGH_VALUE_THRESHOLD) segment = "HIGH_VALUE";
        else if (totalScore >= GROWING_THRESHOLD) segment = "GROWING";
        else segment = "INACTIVE";

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
                customer.getId(), customer.getName(), customer.getPhone(),
                customer.getGiftLevel(), totalScore, segment, dimScores,
                isBirthdaySoon,
                customer.getBirthday() != null ? customer.getBirthday().toString() : null,
                daysToBirthday, customer.getStatus()
        );
    }

    // ==================== 评分方法 ====================

    double scoreTotalSpent(BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) return 0;
        if (total.compareTo(new BigDecimal("10000")) >= 0) return 100;
        if (total.compareTo(new BigDecimal("5000")) >= 0) return 80;
        if (total.compareTo(new BigDecimal("2000")) >= 0) return 60;
        if (total.compareTo(new BigDecimal("500")) >= 0) return 40;
        return 20;
    }

    double scoreFrequency(int count) {
        if (count <= 0) return 0;
        if (count >= 10) return 100;
        return count * 10;
    }

    double scoreRecency(LocalDateTime lastOrder) {
        if (lastOrder == null) return 0;
        long days = ChronoUnit.DAYS.between(lastOrder.toLocalDate(), LocalDate.now());
        if (days <= 7) return 100;
        if (days <= 30) return 80;
        if (days <= 90) return 60;
        if (days <= 180) return 40;
        return 10;
    }

    double scoreGiftLevel(Integer giftLevel) {
        if (giftLevel == null || giftLevel <= 0) return 0;
        return switch (giftLevel) {
            case 1 -> 40;
            case 2 -> 60;
            case 3 -> 80;
            default -> Math.min(giftLevel * 20, 100);
        };
    }

    double scoreTenure(Customer customer) {
        if (customer.getRegisteredAt() == null) return 0;
        long months = ChronoUnit.MONTHS.between(customer.getRegisteredAt(), LocalDate.now());
        if (months >= 12) return 100;
        return Math.max(0, 100 - (12 - months) * 8);
    }

    double scoreReferral(Customer customer) {
        int referralCount = customer.getReferrals() != null ? customer.getReferrals().size() : 0;
        if (customer.hasReferrer()) {
            return 50 + Math.min(referralCount * 15, 50);
        }
        return Math.min(referralCount * 30, 100);
    }
}
