package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.OrderItem;
import com.henashi.inventorycrm.pojo.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderItemRepository 自定义聚合查询测试。
 */
@SpringBootTest
@Transactional
class OrderItemRepositoryTest {

    @Autowired
    private OrderItemRepository repository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager em;

    private Long customerId;

    @BeforeEach
    void setUp() {
        customerId = customerRepository.findById(1L).orElseThrow().getId();

        createOrderItem(1L, BigDecimal.valueOf(100), LocalDateTime.of(2025, 6, 1, 10, 0));
        createOrderItem(1L, BigDecimal.valueOf(200), LocalDateTime.of(2025, 6, 15, 10, 0));
        createOrderItem(1L, BigDecimal.valueOf(300), LocalDateTime.of(2025, 7, 1, 10, 0));
        em.flush();
    }

    private void createOrderItem(Long customerId, BigDecimal totalAmount, LocalDateTime orderTime) {
        Customer customer = customerRepository.findById(customerId).orElseThrow();
        OrderItem item = new OrderItem();
        item.setCustomer(customer);
        item.setProduct(productRepository.findById(1L).orElse(null));
        item.setProductName("测试商品");
        item.setQuantity(1);
        item.setUnitPrice(totalAmount);
        item.setTotalAmount(totalAmount);
        item.setOrderTime(orderTime);
        repository.save(item);
    }

    @Test
    @DisplayName("sumTotalAmountByCustomerId — 客户消费总额")
    void sumTotalAmountByCustomerId() {
        BigDecimal total = repository.sumTotalAmountByCustomerId(customerId);
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(600));
    }

    @Test
    @DisplayName("sumTotalAmountByCustomerId — 无记录客户返回 0")
    void sumTotalAmountByCustomerIdNoData() {
        BigDecimal total = repository.sumTotalAmountByCustomerId(999L);
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("countByCustomerId — 客户消费次数")
    void countByCustomerId() {
        Long count = repository.countByCustomerId(customerId);
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("countByCustomerId — 无记录客户返回 0")
    void countByCustomerIdNoData() {
        Long count = repository.countByCustomerId(999L);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("findMaxOrderTimeByCustomerId — 客户最近消费时间")
    void findMaxOrderTimeByCustomerId() {
        LocalDateTime max = repository.findMaxOrderTimeByCustomerId(customerId);
        assertThat(max).isNotNull();
        assertThat(max).isEqualTo(LocalDateTime.of(2025, 7, 1, 10, 0));
    }

    @Test
    @DisplayName("findMaxOrderTimeByCustomerId — 无记录客户返回 null")
    void findMaxOrderTimeByCustomerIdNoData() {
        LocalDateTime max = repository.findMaxOrderTimeByCustomerId(999L);
        assertThat(max).isNull();
    }

    @Test
    @DisplayName("findByCustomerId — 分页查询")
    void findByCustomerIdPaged() {
        var page = repository.findByCustomerId(customerId,
                org.springframework.data.domain.PageRequest.of(0, 2));
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findByCustomerId — 列表查询")
    void findByCustomerIdList() {
        var list = repository.findByCustomerId(customerId);
        assertThat(list).hasSize(3);
    }
}
