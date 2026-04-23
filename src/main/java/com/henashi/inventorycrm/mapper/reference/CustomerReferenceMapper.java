package com.henashi.inventorycrm.mapper.reference;

import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CustomerReferenceMapper {

    private final CustomerRepository customerRepository;

    // 添加1分钟缓存
    @Cacheable(value = "customers", key = "#customerId",
            unless = "#result == null",
            cacheManager = "shortCache")
    @Named("idToCustomer")
    public Customer idToCustomer(Long customerId) {
        if (customerId == null) {
            return null;
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Mapper转换 客户不存在: " + customerId));
    }

    /**
     * 批量转换
     */
    @Named("idsToCustomers")
    public List<Customer> idsToCustomers(List<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return customerRepository.findAllById(customerIds);
    }

    @Named("dateTimeDefaultNow")
    public LocalDateTime dateTimeDefaultNow(LocalDateTime source) {
        return Objects.requireNonNullElseGet(source, LocalDateTime::now);
    }

    @Named("dateDefaultNow")
    public LocalDate dateDefaultNow(LocalDate source) {
        return Objects.requireNonNullElseGet(source, LocalDate::now);
    }
}
