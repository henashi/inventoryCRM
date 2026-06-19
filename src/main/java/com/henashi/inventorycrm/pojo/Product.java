package com.henashi.inventorycrm.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product")
@SQLRestriction(value = "deleted = false")
@SQLDelete(sql = "update product set deleted = true where id = ?")
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", unique = true, length = 50)
    private String code;

    @Column(name = "category", length = 50)
    private String category;

    @Builder.Default
    @Column(name = "current_stock")
    private Integer currentStock = 0;

    @Builder.Default
    @Column(name = "safe_stock")
    private Integer safeStock = 10;

    @Builder.Default
    @Column(name = "unit", length = 20)
    private String unit = "个";

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "remark", length = 500)
    private String remark;

    @Builder.Default
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<Gift> gifts = new LinkedHashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<InventoryLog> inventoryLogs = new LinkedHashSet<>();

    public boolean isStockLow() {
        return currentStock != null && safeStock != null && currentStock < safeStock;
    }

    public boolean isOutOfStock() {
        return currentStock != null && currentStock <= 0;
    }

    public void increaseStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("增加数量必须大于0");
        }
        this.currentStock = (this.currentStock == null ? 0 : this.currentStock) + quantity;
    }

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
        if (this.code == null || this.code.isBlank()) {
            this.code = generateProductCode();
        }
    }

    private String generateProductCode() {
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "PRO" + timestamp + random;
    }

    @Override
    public String toString() {
        return String.format(
                "Product{id=%d, name='%s', code='%s', category='%s', currentStock=%d, safeStock=%d, unit='%s', price=%s, cost=%s, description='%s', status=%s, remark='%s'}",
                getId(), getName(), getCode(), getCategory(), getCurrentStock(), getSafeStock(), getUnit(), getPrice(), getCost(), getDescription(), getStatus(), getRemark());
    }
}
