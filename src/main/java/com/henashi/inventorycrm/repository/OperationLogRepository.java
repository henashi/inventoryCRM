package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {

    @Query("SELECT o FROM OperationLog o WHERE " +
            "LOWER(o.module) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
//            "LOWER(o.operationType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.operator) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<OperationLog> search(@Param("keyword") String keyword, Pageable pageable);
}
