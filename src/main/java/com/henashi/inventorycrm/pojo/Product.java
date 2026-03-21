package com.henashi.inventorycrm.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

/**
 * 商品实体
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product")
@SQLRestriction(value = "is_deleted = false")
@SQLDelete(sql = "update product set is_deleted = true where id = ?")
public class Product extends BaseEntity {

    /**
     * 商品名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 商品编码（唯一）
     */
    @Column(name = "code", unique = true, length = 50)
    private String code;

    /**
     * 商品分类
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 当前库存
     */
    @Column(name = "current_stock")
    private Integer currentStock = 0;

    /**
     * 安全库存（低于此值预警）
     */
    @Column(name = "safe_stock")
    private Integer safeStock = 10;

    /**
     * 单位（个、件、箱等）
     */
    @Column(name = "unit", length = 20)
    private String unit = "个";

    /**
     * 单价
     */
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 商品描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 商品状态：1-正常 0-停用
     */
    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "remark", length = 500)
    private String remark;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<Gift> gifts = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product")
    private Set<InventoryLog> inventoryLogs = new LinkedHashSet<>();

    // 业务方法
    public boolean isStockLow() {
        return currentStock != null && safeStock != null && currentStock < safeStock;
    }

    public boolean isOutOfStock() {
        return currentStock != null && currentStock <= 0;
    }

    /**
     * 增加库存
     */
    public void increaseStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("增加数量必须大于0");
        }
        this.currentStock = (this.currentStock == null ? 0 : this.currentStock) + quantity;
    }

    /**
     * 减少库存
     */
    public void decreaseStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("减少数量必须大于0");
        }
        if (this.currentStock == null || this.currentStock < quantity) {
            throw new IllegalStateException("库存不足，当前库存：" + this.currentStock);
        }
        this.currentStock -= quantity;
    }

    @PrePersist
    protected void onCreate() {
        if (this.code == null) {
            this.code = generateProductCode();  // 自动生成编码
        }
    }

    private String generateProductCode() {
        // 规则：PRO + 年月日 + 4位随机数，例如：PRO202412150001
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "PRO" + timestamp + random;
    }

    @Override
    public String toString() {
        return "Product{" +
                "name='" + getName() + '\'' +
                ", code='" + getCode() + '\'' +
                ", category='" + getCategory() + '\'' +
                ", currentStock=" + getCurrentStock() +
                ", safeStock=" + getSafeStock() +
                ", unit='" + getUnit() + '\'' +
                ", price=" + getPrice() +
                ", description='" + getDescription() + '\'' +
                ", status=" + getStatus() +
                ", remark='" + getRemark() + '\'' +
                '}';
    }
}