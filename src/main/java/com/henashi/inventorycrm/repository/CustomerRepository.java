package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.Customer;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    boolean existsByPhone(String phone);

    @EntityGraph(value = "Customer.withReferrer")
    @Query("SELECT c FROM Customer c")
    Page<Customer> findAllWithReferrer(@Nullable Specification<Customer> spec, Pageable pageable);
}
