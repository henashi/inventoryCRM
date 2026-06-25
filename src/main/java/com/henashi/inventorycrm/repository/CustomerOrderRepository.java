package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.CustomerOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Page<CustomerOrder> findByCustomerId(Long customerId, Pageable pageable);
}
