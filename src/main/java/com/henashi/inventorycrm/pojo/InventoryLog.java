package com.henashi.inventorycrm.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 库存流水记录实体
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventory_log")
@SQLRestriction(value = "is_deleted = false")
@SQLDelete(sql = "update inventory_log set is_deleted = true where id = ?")
public class InventoryLog extends BaseEntity {

    /**
     * 关联的商品
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 流水类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private LogType type;

    /**
     * 数量（正数）
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 操作前库存
     */
    @Column(name = "before_stock")
    private Integer beforeStock;

    /**
     * 操作后库存
     */
    @Column(name = "after_stock")
    private Integer afterStock;

    /**
     * 原因/备注
     */
    @Column(name = "reason", length = 200)
    private String reason;

    /**
     * 操作人
     */
    @Column(name = "operator", length = 50)
    private String operator = "system";

    @Column(name = "remark", length = 500)
    private String remark;

    // 枚举：流水类型
    @Getter
    public enum LogType {
        IN("入库"),      // 采购入库、退货入库等
        OUT("出库"),     // 销售出库、报损出库等
        ADJUST("调整"),  // 库存调整
        CREATE("新建");   // 盘点调整

        private final String description;

        LogType(String description) {
            this.description = description;
        }

    }

    // 业务方法：创建入库记录
    public static InventoryLog createInLog(Product product, Integer quantity, String reason, String operator, String remark) {
        InventoryLog log = new InventoryLog();
        log.setProduct(product);
        log.setType(LogType.IN);
        log.setQuantity(quantity);
        log.setBeforeStock(product.getCurrentStock());
        log.setAfterStock(product.getCurrentStock() + quantity);
        log.setReason(reason);
        log.setOperator(operator);
        log.setRemark(remark);
        return log;
    }

    // 业务方法：创建出库记录
    public static InventoryLog createOutLog(Product product, Integer quantity, String reason, String operator, String remark) {
        InventoryLog log = new InventoryLog();
        log.setProduct(product);
        log.setType(LogType.OUT);
        log.setQuantity(quantity);
        log.setBeforeStock(product.getCurrentStock());
        log.setAfterStock(product.getCurrentStock() - quantity);
        log.setReason(reason);
        log.setOperator(operator);
        log.setRemark(remark);
        return log;
    }
}
