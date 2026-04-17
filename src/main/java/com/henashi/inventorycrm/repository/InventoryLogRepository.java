package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.dto.InventoryLogTypeStatsDTO;
import com.henashi.inventorycrm.pojo.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long>, JpaSpecificationExecutor<InventoryLog> {

    Page<InventoryLog> findByProductId(Long productId, Pageable pageable);

    Page<InventoryLog> findByType(InventoryLog.LogType type, Pageable pageable);

    @Query("select il.type, sum(il.quantity) quantityCount, count(il) count from InventoryLog il where il.deleted = false group by il.type")
    List<InventoryLogTypeStatsDTO> countStats();
}
