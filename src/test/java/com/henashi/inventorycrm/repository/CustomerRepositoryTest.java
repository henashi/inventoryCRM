package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.Customer;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomerRepository 自定义查询测试。
 * 验证 @EntityGraph、Specification、派生查询。
 */
@SpringBootTest
@Transactional
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository repository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("findAllWithReferrer — @EntityGraph 延迟加载推荐人")
    void findAllWithReferrer() {
        // 创建推荐人（不依赖 seed data，避免并行测试干扰）
        Customer referrer = new Customer();
        referrer.setName("推荐人A");
        referrer.setPhone("13800000999");
        referrer = repository.save(referrer);
        Customer customer = new Customer();
        customer.setName("被推荐客户");
        customer.setPhone("13900000001");
        customer.setReferrer(referrer);
        repository.save(customer);
        em.flush();
        em.clear(); // 清除一级缓存，确保从 DB 重新加载

        // 通过 @EntityGraph 查询 — 应在一次查询中加载 referrer
        var result = repository.findAllWithReferrer(null, Pageable.unpaged());
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(2);

        Customer loaded = result.getContent().stream()
                .filter(c -> "被推荐客户".equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "未找到「被推荐客户」, 总数=" + result.getTotalElements()
                        + ", 内容=" + result.getContent().stream().map(Customer::getName).toList()
                ));
        // 断言 referrer 已加载（非懒加载代理）
        assertThat(loaded.getReferrer()).isNotNull();
        assertThat(loaded.getReferrer().getName()).isEqualTo("推荐人A");
    }

    @Test
    @DisplayName("findAllWithReferrer — 带 Specification 筛选")
    void findAllWithReferrerSpec() {
        Specification<Customer> spec = (root, query, cb) ->
                cb.equal(root.get("name"), "测试客户");

        var result = repository.findAllWithReferrer(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("测试客户");
    }

    @Test
    @DisplayName("existsByPhone — 手机号存在判断")
    void existsByPhone() {
        assertThat(repository.existsByPhone("13800000000")).isTrue();
        assertThat(repository.existsByPhone("99999999999")).isFalse();
    }

    @Test
    @DisplayName("findByPhone — 按手机号查询")
    void findByPhone() {
        var result = repository.findByPhone("13800000000");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("测试客户");

        assertThat(repository.findByPhone("99999999999")).isNotPresent();
    }
}
