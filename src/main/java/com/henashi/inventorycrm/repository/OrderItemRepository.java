package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    Page<OrderItem> findByCustomerId(Long customerId, Pageable pageable);

    List<OrderItem> findByCustomerId(Long customerId);

    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderItem o WHERE o.customer.id = :customerId")
    BigDecimal sumTotalAmountByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT COUNT(o) FROM OrderItem o WHERE o.customer.id = :customerId")
    Long countByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT MAX(o.orderTime) FROM OrderItem o WHERE o.customer.id = :customerId")
    LocalDateTime findMaxOrderTimeByCustomerId(@Param("customerId") Long customerId);
}
