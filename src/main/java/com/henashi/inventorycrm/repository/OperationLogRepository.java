package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
   Page<OperationLog> findByModule(String module, Pageable pageable);

    Page<OperationLog> findByOperator(String operator, Pageable pageable);

    Page<OperationLog> findByStatus(Integer status, Pageable pageable);

    @Query("SELECT o FROM OperationLog o WHERE " +
            "LOWER(o.module) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.operationType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.operator) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<OperationLog> search(@Param("keyword") String keyword, Pageable pageable);
}
