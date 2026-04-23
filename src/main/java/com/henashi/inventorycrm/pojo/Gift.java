package com.henashi.inventorycrm.pojo;

import ch.qos.logback.core.util.StringUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 礼品实体类
 * 继承自BaseEntity，实现Serializable接口
 * 使用Lombok注解简化代码：@Getter, @Setter, @Accessors(chain = true), @ToString
 * 使用JPA注解实现ORM映射：@Entity, @Table, @Column, @Enumerated, @Index, @UniqueConstraint等
 * 使用Hibernate注解实现软删除和动态SQL：@DynamicInsert, @DynamicUpdate, @SQLDelete, @Where
 */
@Accessors(chain = true)
//        (exclude = {"distributions", "logs"})
@Entity
@Table(name = "gift",
        indexes = {
                @Index(name = "idx_gift_name", columnList = "name"),
                @Index(name = "idx_gift_code", columnList = "code"),
                @Index(name = "idx_gift_status", columnList = "giftStatus"),
                @Index(name = "idx_gift_type", columnList = "type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_gift_code", columnNames = "code")
        }
)
@DynamicInsert
@DynamicUpdate
@SQLDelete(sql = "UPDATE gift set deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Gift extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 礼品名称
     * 非空，最大长度100
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 礼品编码（唯一）
     * 非空，最大长度50，必须唯一
     */
    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    /**
     * 礼品类型
     * 非空，使用字符串枚举类型
     * 默认值为PHYSICAL（实体礼品）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private GiftType type = GiftType.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    /**
     * 礼品描述
     * 最大长度500
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 礼品状态
     * 非空，使用字符串枚举类型
     * 默认值为DRAFT（草稿）
     */
    @Transient
    private GiftStatus giftStatus;

    /**
     * 是否限制领取次数
     * 非空，默认值为false
     */
    @Column(name = "limit_enabled", nullable = false)
    private Boolean limitEnabled = false;

    /**
     * 每人限领次数
     * 当limitEnabled为true时生效
     */
    @Column(name = "limit_per_person")
    private Integer limitPerPerson = 1;

    /**
     * 备注
     * 最大长度500
     */
    @Column(name = "remark", length = 200)
    private String remark;

    @PrePersist
    private void generateCodeBeforeCreate() {
        if (StringUtil.isNullOrEmpty(getCode())) {
            setCode(generateGiftCode());
        }
        if (giftStatus != null) {
            setStatus(giftStatus.name());
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (giftStatus != null) {
          setStatus(giftStatus.name());
        }
    }

    @PostLoad
    public void postLoad() {
        if(getStatus() != null) {
            this.giftStatus = GiftStatus.valueOf(getStatus());
        }
    }

    public GiftStatus getGiftStatus() {
        if (giftStatus == null && this.getStatus() != null) {
            giftStatus = GiftStatus.valueOf(this.getStatus());
        }
        return giftStatus;
    }

    public void setGiftStatus(GiftStatus giftStatus) {
        this.giftStatus = giftStatus;
        if (giftStatus != null) {
            setStatus(giftStatus.name());
        }
        else {
            setStatus(null);
        }
    }

    private String generateGiftCode() {
        // 规则：GIFT + 年月日 + 4位随机数，例如：PRO202412150001
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "GIFT" + timestamp + random;
    }

    /**
     * 礼品类型枚举
     * 定义了四种礼品类型：实体礼品、虚拟礼品、优惠券、积分
     */
    @Getter
    public enum GiftType {
        NEW("邀新礼品"),
        PHYSICAL("实体礼品"),
        VIRTUAL("虚拟礼品"),
        COUPON("优惠券"),
        POINTS("积分");

        private final String description;

        GiftType(String description) {
            this.description = description;
        }

        public static GiftType fromString(String value) {
            for (GiftType type : GiftType.values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("未知的礼品类型: " + value);
        }
    }

    /**
     * 礼品状态枚举
     * 定义了五种礼品状态：草稿、生效中、已暂停、已过期、已发完
     */
    @Getter
    public enum GiftStatus {
        DRAFT("草稿"),
        ACTIVE("生效中"),
        PAUSED("已暂停"),
        EXPIRED("已过期"),
        DEPLETED("已发完");

        private final String description;

        GiftStatus(String description) {
            this.description = description;
        }

        public static GiftStatus fromString(String value) {
            for (GiftStatus status : GiftStatus.values()) {
                if (status.name().equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("未知的礼品状态: " + value);
        }
    }

    // ==================== 业务方法 ====================

    /**
     * 校验业务规则
     * 检查礼品名称、编码、数量、价值等字段的合法性
     */
    public void validateBusinessRules() {
        if (this.name == null || this.name.trim().length() < 2) {
            throw new IllegalStateException("礼品名称不能少于2个字符");
        }

        if (this.name.trim().length() > 100) {
            throw new IllegalStateException("礼品名称不能超过100个字符");
        }

        if (this.code == null || this.code.trim().isEmpty()) {
            throw new IllegalStateException("礼品编码不能为空");
        }

        if (!this.code.matches("^[A-Z0-9-]+$")) {
            throw new IllegalStateException("礼品编码只能包含大写字母、数字和横线");
        }

        if (this.limitEnabled && (this.limitPerPerson == null || this.limitPerPerson <= 0)) {
            throw new IllegalStateException("限制领取次数时，每人限领次数必须大于0");
        }
    }

    /**
     * 检查礼品是否可发放
     * 检查状态、库存、时间等条件是否满足发放要求
     * @return 是否可发放
     */
    public boolean isDistributable() {
        // 状态检查
        return this.giftStatus == GiftStatus.ACTIVE;
    }

    /**
     * 发放礼品
     * @param amount 发放数量
     */
    public synchronized void distribute(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("发放数量必须大于0");
        }

        if (!isDistributable()) {
            throw new IllegalStateException("礼品当前不可发放");
        }
    }

    /**
     * 激活礼品
     * 将礼品状态设置为生效中
     */
    public void activate() {
        if (this.giftStatus == GiftStatus.ACTIVE) {
            return;
        }

        this.giftStatus = GiftStatus.ACTIVE;
    }

    /**
     * 暂停礼品
     * 将礼品状态设置为已暂停
     */
    public void pause() {
        if (this.giftStatus == GiftStatus.PAUSED) {
            return;
        }

        if (this.giftStatus != GiftStatus.ACTIVE) {
            throw new IllegalStateException("只有生效中的礼品可以暂停");
        }

        this.giftStatus = GiftStatus.PAUSED;
    }

    /**
     * 恢复礼品
     * 将礼品状态从已暂停恢复为生效中
     */
    public void resume() {
        if (this.giftStatus == GiftStatus.ACTIVE) {
            return;
        }

        if (this.giftStatus != GiftStatus.PAUSED) {
            throw new IllegalStateException("只有暂停的礼品可以恢复");
        }

        this.giftStatus = GiftStatus.ACTIVE;
    }

    /**
     * 检查是否超过限制次数
     * @param currentCount 当前领取次数
     * @return 是否超过限制
     */
    public boolean exceedsLimit(Integer currentCount) {
        if (!Boolean.TRUE.equals(this.limitEnabled) || this.limitPerPerson == null) {
            return false;
        }

        return currentCount != null && currentCount >= this.limitPerPerson;
    }

    /**
     * 获取状态描述
     * @return 状态描述
     */
    public String getStatusDescription() {
        return this.giftStatus.getDescription();
    }

    /**
     * 获取类型描述
     * @return 类型描述
     */
    public String getTypeDescription() {
        return this.type.getDescription();
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建立即生效的礼品
     * @param name 礼品名称
     * @param code 礼品编码
     * @param type 礼品类型
     * @return 礼品对象
     */
    public static Gift createActive(String name, String code, GiftType type) {
        Gift gift = new Gift();
        gift.setName(name);
        gift.setCode(code);
        gift.setType(type);
        gift.setGiftStatus(GiftStatus.ACTIVE);
        gift.validateBusinessRules();
        return gift;
    }

    /**
     * 创建带时间范围的礼品
     * @param name 礼品名称
     * @param code 礼品编码
     * @param type 礼品类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 礼品对象
     */
    public static Gift createWithTimeRange(String name, String code, GiftType type,
                                           LocalDateTime startTime,
                                           LocalDateTime endTime) {
        Gift gift = new Gift();
        gift.setName(name);
        gift.setCode(code);
        gift.setType(type);
        gift.setGiftStatus(startTime == null || !LocalDateTime.now().isBefore(startTime)
                ? GiftStatus.ACTIVE : GiftStatus.DRAFT);
        gift.validateBusinessRules();
        return gift;
    }

    @Override
    public String toString() {
        return String.format(
                "Gift{name='%s', code='%s', type=%s, product=%s, description='%s', giftStatus=%s, limitEnabled=%s, limitPerPerson=%d, remark='%s'}",
                getName(),
                getCode(),
                getType(),
                getProduct() != null ? getProduct().getId() : "null",  // 安全处理
                getDescription(),
                getGiftStatus(),
                getLimitEnabled(),
                getLimitPerPerson(),
                getRemark()
        );
    }
}