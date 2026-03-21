package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    boolean existsByPhone(String phone);

    boolean existsCustomerById(Long id);

    Page<Customer> findByNameContainingIgnoreCaseOrPhoneEndingWithOrEmailContainingIgnoreCase(String kw, String kw1, String kw2, Pageable pageable);
}
