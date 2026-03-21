package com.henashi.inventorycrm.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户实体（包含推荐关系）
 */
@Entity
@Getter
@Setter
@Table(
        name = "customer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_phone_deleted", columnNames = {"phone", "is_deleted"})
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLRestriction(value = "is_deleted = false")
@SQLDelete(sql = "update customer set is_deleted = true where id = ?")
public class Customer extends BaseEntity{

    /**
     * 客户姓名
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 手机号（唯一，作为登录账号）
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 地址
     */
    @Column(name = "address", length = 200)
    private String address;

    /**
     * 邮箱
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 客户类型：1-普通客户 2-会员
     */
    @Column(name = "type")
    private Integer type = 1;

    /**
     * 推荐人（自关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id")
    private Customer referrer;

    /**
     * 被推荐人列表（一对多关系）
     */
    @OneToMany(mappedBy = "referrer", fetch = FetchType.LAZY)
    private List<Customer> referrals = new ArrayList<>();

    /**
     * 礼品等级：0-未领取 1-已领一级礼 2-已领二级礼 3-已领三级礼
     */
    @Column(name = "gift_level")
    private Integer giftLevel = 0;

    /**
     * 最近一次领取礼品时间
     */
    @Column(name = "gift_received_at")
    private LocalDateTime giftReceivedAt;

    /**
     * 注册时间
     */
    @Column(name = "registered_at")
    private LocalDate registeredAt = LocalDate.now();

    /**
     * 客户状态：1-正常 0-停用
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 性别字段
     */
    @Column(name = "gender")
    private Integer gender = 0;  // 0: 女, 1: 男, null: 未设置

    /**
     * 生日字段
     */
    @Column(name = "birthday")
    private LocalDate birthday;

    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 是否可以领取礼品
     */
    public boolean canReceiveGift() {
        return giftLevel < 3;
    }

    /**
     * 领取礼品
     * @return 领取后的礼品等级
     */
    public Integer receiveGift() {
        if (!canReceiveGift()) {
            throw new IllegalStateException("该客户已达到礼品领取上限");
        }
        this.giftLevel++;
        this.giftReceivedAt = LocalDateTime.now();
        return this.giftLevel;
    }

    /**
     * 获取推荐人数
     */
    public int getReferralCount() {
        return referrals != null ? referrals.size() : 0;
    }

    /**
     * 是否有推荐人
     */
    public boolean hasReferrer() {
        return referrer != null;
    }

    /**
     * 获取当前可领取的礼品等级
     */
    public Integer getNextGiftLevel() {
        return giftLevel + 1;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", phone='" + getPhone() + '\'' +
                ", address='" + getAddress() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", type=" + getType() +
                ", referrer=" + (getReferrer() != null ? getReferrer().getId() : "") +
                ", giftLevel=" + getGiftLevel() +
                ", giftReceivedAt=" + getGiftReceivedAt() +
                ", registeredAt=" + getRegisteredAt() +
                ", remark='" + getRemark() + '\'' +
                ", status=" + getStatus() +
                '}';
    }
}