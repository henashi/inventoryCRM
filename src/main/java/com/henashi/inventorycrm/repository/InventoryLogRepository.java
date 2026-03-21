package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long>, JpaSpecificationExecutor<InventoryLog> {

    Page<InventoryLog> findByProductId(Long productId, Pageable pageable);

    Page<InventoryLog> findByType(InventoryLog.LogType type, Pageable pageable);
}
