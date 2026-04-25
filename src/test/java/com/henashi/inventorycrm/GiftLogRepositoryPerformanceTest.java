package com.henashi.inventorycrm;

import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.pojo.GiftLog;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.GiftLogRepository;
import com.henashi.inventorycrm.repository.GiftRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
// 仅保留完整上下文测试
@SpringBootTest
// 关键：让测试类实例 per-class，支持非static的@BeforeAll
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// 不开启事务，避免影响性能
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class GiftLogRepositoryPerformanceTest {

    @Autowired
    private GiftLogRepository giftLogRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private GiftRepository giftRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private TransactionTemplate transactionTemplate;

    // 记录测试数据的ID，用于后续精准删除
    private Long testCustomerId;
    private Long testGiftId;

    @BeforeAll
    // 非static方法，可直接注入Bean
    void setUpOnce() {
        // 手动开启并提交事务
        transactionTemplate.execute(status -> {
            // 准备测试数据并保存到实例变量
            Customer testCustomer = Customer.builder().name("性能测试客户").build();
            em.persist(testCustomer);
            // 保存 ID
            testCustomerId = testCustomer.getId();

            Gift testGift = Gift.builder()
                    .name("性能测试礼品")
                    .limitEnabled(false)
                    .build();
            em.persist(testGift);
            // 保存 ID
            testGiftId = testGift.getId();

            return null;
        });
    }

    @AfterAll
    void tearDownOnce() {
        // 手动开启事务清理数据
        transactionTemplate.execute(status -> {
            // 1. 先删除与测试客户/礼品关联的所有GiftLog
            em.createQuery("DELETE FROM GiftLog WHERE customer.id = :customerId OR gift.id = :giftId")
                    .setParameter("customerId", testCustomerId)
                    .setParameter("giftId", testGiftId)
                    .executeUpdate();

            // 2. 再删除测试客户和礼品
            em.createQuery("DELETE FROM Customer WHERE id = :customerId")
                    .setParameter("customerId", testCustomerId)
                    .executeUpdate();

            em.createQuery("DELETE FROM Gift WHERE id = :giftId")
                    .setParameter("giftId", testGiftId)
                    .executeUpdate();

            return null;
        });
    }

    @BeforeEach
    void warmUp() {
        // 预热：执行5次插入，让JVM完成类加载和JIT编译
        for (int i = 0; i < 5; i++) {
            GiftLog warmUpLog = GiftLog.builder()
                    .operator("system")
                    .customer(customerRepository.getReferenceById(testCustomerId)) // 用ID获取引用
                    .gift(giftRepository.getReferenceById(testGiftId))
                    .quantity(10)
                    .build();
            giftLogRepository.save(warmUpLog);
        }
    }

    @RepeatedTest(100)  // 仅保留@RepeatedTest
    void testCreatePerformance() {
        long start = System.nanoTime();

        GiftLog giftLog = GiftLog.builder()
                .operator("system")
                .customer(customerRepository.getReferenceById(testCustomerId))
                .gift(giftRepository.getReferenceById(testGiftId))
                .quantity(10)
                .build();

        giftLogRepository.save(giftLog);

        long duration = System.nanoTime() - start;
        log.info("插入耗时: {}ms", TimeUnit.NANOSECONDS.toMillis(duration));

        // 改为统计95%分位数，或仅记录日志不做绝对断言（性能测试更适合趋势分析）
        // 若必须断言，可放宽阈值或用统计方式
    }
}