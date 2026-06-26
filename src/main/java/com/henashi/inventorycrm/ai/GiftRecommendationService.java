package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.GiftRecommendationDTO;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.GiftLogRepository;
import com.henashi.inventorycrm.repository.GiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 礼品推荐服务
 * <p>
 * 基于客户评分和礼品属性，匹配最佳礼品并生成推荐理由。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GiftRecommendationService {

    private final GiftRepository giftRepository;
    private final GiftLogRepository giftLogRepository;
    private final CustomerRepository customerRepository;

    /**
     * 为指定客户推荐 Top-3 礼品
     */
    public List<GiftRecommendationDTO> recommendForCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) return List.of();

        // 获取客户已领取的礼品 ID 列表
        Set<Long> receivedGiftIds = findReceivedGiftIds(customerId);

        // 获取所有生效中的礼品（兼容 status 字符串和 giftStatus 枚举）
        List<Gift> activeGifts = giftRepository.findAll().stream()
                .filter(g -> {
                    if (g.getGiftStatus() == Gift.GiftStatus.ACTIVE) return true;
                    if ("ACTIVE".equals(g.getStatus())) return true;
                    return false;
                })
                .filter(g -> !receivedGiftIds.contains(g.getId()))
                .collect(Collectors.toList());

        return activeGifts.stream()
                .map(gift -> {
                    double matchScore = calculateMatchScore(customer, gift);
                    String reason = generateReason(customer, gift, matchScore);
                    return new GiftRecommendationDTO(
                            gift.getId(),
                            gift.getName(),
                            gift.getCode(),
                            gift.getType() != null ? gift.getType().name() : "",
                            Math.round(matchScore * 100.0) / 100.0,
                            reason
                    );
                })
                .filter(r -> r.matchScore() > 0)
                .sorted(Comparator.comparingDouble(GiftRecommendationDTO::matchScore).reversed())
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * 计算礼品匹配度 (0-100)
     * <p>
     * 考虑因素：
     * - 客户礼品等级：等级越高，可匹配更高价值礼品
     * - 礼品类型偏好：根据历史领取类型判断偏好
     * - 生日加分：生日临近时相关礼品加分
     */
    double calculateMatchScore(Customer customer, Gift gift) {
        double score = 60; // 基础分

        // 礼品等级匹配：客户等级越高，能匹配的礼品范围越大
        int giftLevel = customer.getGiftLevel() != null ? customer.getGiftLevel() : 0;
        if (giftLevel >= 2) score += 20;
        else if (giftLevel >= 1) score += 10;

        // 生日加分
        if (customer.getBirthday() != null) {
            LocalDate today = LocalDate.now();
            LocalDate nextBirthday = customer.getBirthday().withYear(today.getYear());
            if (nextBirthday.isBefore(today)) nextBirthday = nextBirthday.plusYears(1);
            long daysToBirthday = ChronoUnit.DAYS.between(today, nextBirthday);
            if (daysToBirthday <= 7) score += 20;
            else if (daysToBirthday <= 30) score += 10;
        }

        // 新客户偏好 NEW 类型礼品
        if (giftLevel == 0 && gift.getType() == Gift.GiftType.NEW) {
            score += 15;
        }

        // 活跃客户偏好优惠券/积分
        long daysSinceLastGift = customer.getGiftReceivedAt() != null
                ? ChronoUnit.DAYS.between(customer.getGiftReceivedAt().toLocalDate(), LocalDate.now())
                : 999;
        if (daysSinceLastGift <= 30 && (gift.getType() == Gift.GiftType.COUPON || gift.getType() == Gift.GiftType.POINTS)) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    /**
     * 生成推荐理由
     */
    String generateReason(Customer customer, Gift gift, double matchScore) {
        int giftLevel = customer.getGiftLevel() != null ? customer.getGiftLevel() : 0;
        StringBuilder sb = new StringBuilder();

        if (matchScore >= 80) {
            sb.append("高度匹配");
        } else if (matchScore >= 60) {
            sb.append("推荐");
        } else {
            sb.append("可考虑");
        }

        // 基于客户特征补充理由
        if (customer.getBirthday() != null) {
            LocalDate today = LocalDate.now();
            LocalDate nextBirthday = customer.getBirthday().withYear(today.getYear());
            if (nextBirthday.isBefore(today)) nextBirthday = nextBirthday.plusYears(1);
            long days = ChronoUnit.DAYS.between(today, nextBirthday);
            if (days <= 7) {
                sb.append("，生日在即 🎂");
            }
        }

        if (customer.getGiftReceivedAt() != null) {
            long daysSinceLastGift = ChronoUnit.DAYS.between(customer.getGiftReceivedAt().toLocalDate(), LocalDate.now());
            if (daysSinceLastGift > 60) {
                sb.append("，已 ").append(daysSinceLastGift).append(" 天未领取礼品");
            }
        } else {
            sb.append("，尚未领取过礼品");
        }

        if (giftLevel >= 2) {
            sb.append("，高等级客户推荐优质礼品");
        }

        return sb.toString();
    }

    private Set<Long> findReceivedGiftIds(Long customerId) {
        return giftLogRepository.findByCustomerId(customerId,
                        org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getContent().stream()
                .map(log -> log.getGift().getId())
                .collect(Collectors.toSet());
    }
}
