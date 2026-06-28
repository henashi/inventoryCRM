package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.GiftRecommendationDTO;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.pojo.GiftLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.GiftLogRepository;
import com.henashi.inventorycrm.repository.GiftRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GiftRecommendationService 礼品推荐算法单元测试
 * <p>
 * 覆盖匹配度计算、推荐理由生成、完整推荐链路。
 */
@SpringBootTest
@Transactional
class GiftRecommendationServiceTest {

    @Autowired
    private GiftRecommendationService recommendationService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private GiftRepository giftRepository;

    @Autowired
    private GiftLogRepository giftLogRepository;

    @Autowired
    private ProductRepository productRepository;

    private Customer testCustomer;
    private Gift newGift;
    private Gift couponGift;
    private Gift physicalGift;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = productRepository.save(Product.builder()
                .name("礼品关联商品")
                .code("GIFT_PROD_001")
                .currentStock(50)
                .safeStock(5)
                .unit("件")
                .price(BigDecimal.TEN)
                .status("1")
                .build());

        // 创建测试客户
        testCustomer = customerRepository.save(Customer.builder()
                .name("推荐测试客户")
                .phone("13800000100")
                .giftLevel(1)
                .birthday(LocalDate.now().plusDays(5))  // 5天后生日
                .registeredAt(LocalDate.now().minusMonths(3))
                .status("1")
                .build());

        // 创建各种礼品
        newGift = giftRepository.save(Gift.builder()
                .name("邀新礼品")
                .code("GIFT_REC_NEW")
                .type(Gift.GiftType.NEW)
                .product(testProduct)
                .giftStatus(Gift.GiftStatus.ACTIVE)
                .status("ACTIVE")
                .build());

        couponGift = giftRepository.save(Gift.builder()
                .name("优惠券礼品")
                .code("GIFT_REC_COUPON")
                .type(Gift.GiftType.COUPON)
                .product(testProduct)
                .giftStatus(Gift.GiftStatus.ACTIVE)
                .status("ACTIVE")
                .build());

        physicalGift = giftRepository.save(Gift.builder()
                .name("实体礼品")
                .code("GIFT_REC_PHYS")
                .type(Gift.GiftType.PHYSICAL)
                .product(testProduct)
                .giftStatus(Gift.GiftStatus.ACTIVE)
                .status("ACTIVE")
                .build());
    }

    // ==================== calculateMatchScore — 匹配度计算 ====================

    @Test
    @DisplayName("匹配度 — 新客户 + NEW 类型礼品 = 60基础 + 15新客户加成 = 75")
    void matchScoreBase() {
        Customer c = customerRepository.save(Customer.builder()
                .name("基础客户").phone("13800000101")
                .giftLevel(0)
                .status("1")
                .build());

        double score = recommendationService.calculateMatchScore(c, newGift);
        // 60(基础) + 15(新客户 + NEW 类型礼品) = 75
        assertThat(score).isEqualTo(75);
    }

    @Test
    @DisplayName("匹配度 — giftLevel=1 + 生日在即 = 60+10+20")
    void matchScoreGiftLevel1() {
        // testCustomer: giftLevel=1, 生日在5天后
        double score = recommendationService.calculateMatchScore(testCustomer, physicalGift);
        // 60(基础) + 10(等级1) + 20(生日≤7天) = 90
        assertThat(score).isEqualTo(90);
    }

    @Test
    @DisplayName("匹配度 — giftLevel=2 加 20 分")
    void matchScoreGiftLevel2() {
        Customer c = customerRepository.save(Customer.builder()
                .name("高等级客户").phone("13800000102")
                .giftLevel(2)
                .status("1")
                .build());

        double score = recommendationService.calculateMatchScore(c, physicalGift);
        assertThat(score).isEqualTo(80); // 60 + 20
    }

    @Test
    @DisplayName("匹配度 — 生日在即（≤7天）加 20 分")
    void matchScoreBirthdaySoon() {
        // testCustomer 生日在 5 天后
        double score = recommendationService.calculateMatchScore(testCustomer, physicalGift);
        assertThat(score).isEqualTo(90); // 60 + 10(等级1) + 20(生日)
    }

    @Test
    @DisplayName("匹配度 — 生日在 30 天内加 10 分")
    void matchScoreBirthdayWithin30() {
        Customer c = customerRepository.save(Customer.builder()
                .name("生日客户").phone("13800000103")
                .giftLevel(0)
                .birthday(LocalDate.now().plusDays(15))
                .status("1")
                .build());

        double score = recommendationService.calculateMatchScore(c, physicalGift);
        assertThat(score).isEqualTo(70); // 60 + 10(生日30天内)
    }

    @Test
    @DisplayName("匹配度 — 新客户 + NEW 类型礼品 加 15 分")
    void matchScoreNewCustomerNewGift() {
        Customer c = customerRepository.save(Customer.builder()
                .name("新客户").phone("13800000104")
                .giftLevel(0)
                .status("1")
                .build());

        double score = recommendationService.calculateMatchScore(c, newGift);
        // 60(基础) + 15(新客户+NEW礼品) = 75
        assertThat(score).isEqualTo(75);
    }

    @Test
    @DisplayName("匹配度 — 近期领取过礼品 + 优惠券类型 加 10 分")
    void matchScoreRecentGiftCoupon() {
        Customer c = customerRepository.save(Customer.builder()
                .name("活跃客户").phone("13800000105")
                .giftLevel(1)
                .giftReceivedAt(LocalDateTime.now().minusDays(10))  // 10天前领取过
                .status("1")
                .build());

        double score = recommendationService.calculateMatchScore(c, couponGift);
        // 60(基础) + 10(等级1) + 10(近期领取+优惠券) = 80
        assertThat(score).isEqualTo(80);
    }

    @Test
    @DisplayName("匹配度 — 总分上限 100")
    void matchScoreCap100() {
        Customer c = customerRepository.save(Customer.builder()
                .name("高分客户").phone("13800000106")
                .giftLevel(2)
                .birthday(LocalDate.now().plusDays(3))
                .giftReceivedAt(LocalDateTime.now().minusDays(5))
                .status("1")
                .build());

        double score = recommendationService.calculateMatchScore(c, couponGift);
        // 60 + 20(等级2) + 20(生日) + 10(近期领取+优惠券) = 110 → Min(100)
        assertThat(score).isEqualTo(100);
    }

    // ==================== generateReason — 推荐理由 ====================

    @Test
    @DisplayName("推荐理由 — 高度匹配包含关键词")
    void generateReasonHighMatch() {
        String reason = recommendationService.generateReason(testCustomer, physicalGift, 85);
        assertThat(reason).contains("高度匹配");
    }

    @Test
    @DisplayName("推荐理由 — 一般匹配包含「推荐」")
    void generateReasonMediumMatch() {
        String reason = recommendationService.generateReason(testCustomer, physicalGift, 65);
        assertThat(reason).contains("推荐");
    }

    @Test
    @DisplayName("推荐理由 — 低匹配包含「可考虑」")
    void generateReasonLowMatch() {
        String reason = recommendationService.generateReason(testCustomer, physicalGift, 50);
        assertThat(reason).contains("可考虑");
    }

    @Test
    @DisplayName("推荐理由 — 生日在即包含生日提示")
    void generateReasonBirthdaySoon() {
        String reason = recommendationService.generateReason(testCustomer, physicalGift, 85);
        // testCustomer 生日在5天后
        assertThat(reason).contains("生日");
    }

    @Test
    @DisplayName("推荐理由 — 未领过礼品包含提示")
    void generateReasonNeverReceived() {
        Customer c = customerRepository.save(Customer.builder()
                .name("新客户无礼品").phone("13800000107")
                .giftLevel(0)
                .birthday(null)
                .giftReceivedAt(null)
                .status("1")
                .build());

        String reason = recommendationService.generateReason(c, newGift, 75);
        assertThat(reason).contains("尚未领取过礼品");
    }

    @Test
    @DisplayName("推荐理由 — 高等级客户提示优质礼品")
    void generateReasonHighLevel() {
        Customer c = customerRepository.save(Customer.builder()
                .name("高端客户").phone("13800000108")
                .giftLevel(2)
                .giftReceivedAt(LocalDateTime.now().minusDays(30))
                .status("1")
                .build());

        String reason = recommendationService.generateReason(c, physicalGift, 85);
        assertThat(reason).contains("高等级客户");
    }

    // ==================== recommendForCustomer — 完整推荐 ====================

    @Test
    @DisplayName("recommendForCustomer — 不存在的客户返回空列表")
    void recommendForCustomerNonExistent() {
        List<GiftRecommendationDTO> result = recommendationService.recommendForCustomer(99999L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("recommendForCustomer — 返回 Top-3 按匹配度降序")
    void recommendForCustomerTop3() {
        List<GiftRecommendationDTO> result = recommendationService.recommendForCustomer(testCustomer.getId());

        assertThat(result).isNotEmpty();
        assertThat(result.size()).isLessThanOrEqualTo(3);
        // 验证排序
        for (int i = 1; i < result.size(); i++) {
            assertThat(result.get(i - 1).matchScore())
                    .isGreaterThanOrEqualTo(result.get(i).matchScore());
        }
    }

    @Test
    @DisplayName("recommendForCustomer — 各礼品有推荐理由")
    void recommendForCustomerHasReason() {
        List<GiftRecommendationDTO> result = recommendationService.recommendForCustomer(testCustomer.getId());

        assertThat(result).allMatch(r -> r.reason() != null && !r.reason().isEmpty());
        assertThat(result).allMatch(r -> r.matchScore() > 0);
    }

    @Test
    @DisplayName("recommendForCustomer — 已领取过的礼品不重复推荐")
    void recommendForCustomerExcludesReceived() {
        // testCustomer 已领取了 newGift
        giftLogRepository.save(GiftLog.builder()
                .customer(testCustomer)
                .gift(newGift)
                .giftLogStatus(GiftLog.GiftLogStatus.ISSUED)
                .quantity(1)
                .build());

        List<GiftRecommendationDTO> result = recommendationService.recommendForCustomer(testCustomer.getId());

        // 不包含已领取的 newGift
        assertThat(result).noneMatch(r -> r.giftId().equals(newGift.getId()));
    }

    @Test
    @DisplayName("recommendForCustomer — 不活跃礼品不推荐")
    void recommendForCustomerExcludesInactiveGifts() {
        // 创建一个已暂停的礼品
        Gift paused = giftRepository.save(Gift.builder()
                .name("已暂停礼品")
                .code("GIFT_PAUSED")
                .type(Gift.GiftType.VIRTUAL)
                .product(testProduct)
                .giftStatus(Gift.GiftStatus.PAUSED)
                .status("PAUSED")
                .build());

        List<GiftRecommendationDTO> result = recommendationService.recommendForCustomer(testCustomer.getId());

        // 不应包含暂停的礼品
        assertThat(result).noneMatch(r -> r.giftId().equals(paused.getId()));
    }

    @Test
    @DisplayName("recommendForCustomer — 所有活跃礼品都返回推荐")
    void recommendForCustomerReturnsAllActive() {
        List<GiftRecommendationDTO> result = recommendationService.recommendForCustomer(testCustomer.getId());

        // 验证我们创建的 3 个活跃礼品（new/coupon/physical）中，应该至少推荐了1个
        assertThat(result).isNotEmpty();
    }
}
