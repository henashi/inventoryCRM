package com.henashi.inventorycrm.pojo;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 礼品发放记录实体
 */
@Entity
@Table(name = "gift_log",
        indexes = {
                @Index(name = "idx_gift_id", columnList = "gift_id"),
                @Index(name = "idx_gift_log_status", columnList = "giftStatus"),
                @Index(name = "idx_gift_log_customer", columnList = "customer_id")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
@SQLDelete(sql = "update gift_log set deleted = true where id = ?")
public class GiftLog extends BaseEntity {

    /**
     * 领取礼品的客户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * 礼品
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_id", nullable = false)
    private Gift gift;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Transient
    private GiftLogStatus giftLogStatus;

    /**
     * 发放时间
     */
    @CreationTimestamp
    @Column(name = "issue_at", updatable = false)
    private LocalDateTime issueAt;

    @Column(name = "issue_notes", length = 50, comment = "处理说明")
    private String issueNotes;

    /**
     * 操作人
     */
    @Column(name = "operator", length = 50)
    private String operator = "system";

    /**
     * 备注
     */
    @Column(name = "remark", length = 200)
    private String remark;

    @Getter
    public enum GiftLogStatus {
        PENDING("待发放"),
        ISSUED("已发放"),
        CANCELLED("已取消");

        private final String description;

        GiftLogStatus(String description) {
            this.description = description;
        }

    }

    @PrePersist
    public void prePersist() {
        if (giftLogStatus == null) {
            giftLogStatus = GiftLogStatus.PENDING;
        }
        setStatus(giftLogStatus.name());
    }

    @PreUpdate
    public void preUpdate() {
        if (giftLogStatus != null) {
            setStatus(giftLogStatus.name());
        }
    }

    @PostLoad
    public void postLoad() {
        if (getStatus() != null) {
            setGiftLogStatus(GiftLogStatus.valueOf(getStatus()));
        }
    }

    public void setGiftLogStatus(GiftLogStatus logStatus) {
        this.giftLogStatus = logStatus;
        setStatus(logStatus.name());
    }

    public GiftLogStatus getGiftLogStatus() {
        if (giftLogStatus == null && getStatus() != null) {
            giftLogStatus = GiftLogStatus.valueOf(getStatus());
        }
        return giftLogStatus;
    }

    public void issue(String notes) {
        if (this.giftLogStatus != GiftLogStatus.PENDING) {
            throw new IllegalStateException("只有待发放的记录才能发放");
        }
        this.giftLogStatus = GiftLogStatus.ISSUED;
        this.issueNotes = notes;
    }

    public void cancel(String reason) {
        if (this.giftLogStatus != GiftLogStatus.PENDING) {
            throw new IllegalStateException("只有待发放的记录才能取消");
        }
        this.giftLogStatus = GiftLogStatus.CANCELLED;
        this.issueNotes = reason;
    }
}