package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.GiftLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GiftLogRepository extends JpaRepository<GiftLog, Long> {
    Page<GiftLog> findByCustomerId(Long customerId, Pageable pageable);

//    Page<GiftLog> findByGiftLevel(Integer giftLevel, Pageable pageable);
}
