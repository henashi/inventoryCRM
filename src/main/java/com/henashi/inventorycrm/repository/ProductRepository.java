package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByCode(String code);

    Page<Product> findByNameContainingOrCodeContainingIgnoreCaseAndStatusNot(String name, String code, Integer status, Pageable pageable);

    Page<Product> findByStatusNot(Integer status, Pageable pageable);
}
