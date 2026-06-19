package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsByCode(String code);

    Optional<Product> findByCode(String code);

    Page<Product> findByNameContainingOrCodeContainingIgnoreCaseAndStatusNot(String name, String code, Integer status, Pageable pageable);

    Page<Product> findByStatusNot(Integer status, Pageable pageable);

    @Query("select distinct trim(p.category) from Product p where p.category is not null and trim(p.category) <> '' order by trim(p.category) asc")
    List<String> findDistinctCategories();
}
