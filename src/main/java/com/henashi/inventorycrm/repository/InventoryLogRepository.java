package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.dto.InventoryLogTypeStatsDTO;
import com.henashi.inventorycrm.pojo.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long>, JpaSpecificationExecutor<InventoryLog> {

    Page<InventoryLog> findByProductId(Long productId, Pageable pageable);

    Page<InventoryLog> findByType(InventoryLog.LogType type, Pageable pageable);

    /**
     * 查询指定商品在时间范围内的出库记录
     */
    @Query("select il from InventoryLog il where il.product.id = :productId and il.type = 'OUT' and il.createdTime >= :since order by il.createdTime asc")
    List<InventoryLog> findOutLogsByProductSince(@Param("productId") Long productId, @Param("since") LocalDateTime since);

    /**
     * 查询所有商品在时间范围内的出库记录（按商品分组）
     */
    @Query("select il from InventoryLog il where il.type = 'OUT' and il.createdTime >= :since order by il.product.id, il.createdTime asc")
    List<InventoryLog> findAllOutLogsSince(@Param("since") LocalDateTime since);

    @Query("select il.type, sum(il.quantity) quantityCount, count(il) count from InventoryLog il where il.deleted = false group by il.type")
    List<InventoryLogTypeStatsDTO> countStats();

    @Query("select count(il) from InventoryLog il where il.deleted = false and il.status = :status")
    Long countByStatus(@Param("status") String status);
}
