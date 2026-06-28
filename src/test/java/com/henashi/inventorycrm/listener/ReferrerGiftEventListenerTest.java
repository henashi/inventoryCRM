package com.henashi.inventorycrm.listener;

import com.henashi.inventorycrm.event.ReferrerGiftEvent;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.DataDict;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.pojo.GiftLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.DataDictRepository;
import com.henashi.inventorycrm.repository.GiftLogRepository;
import com.henashi.inventorycrm.repository.GiftRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReferrerGiftEventListenerTest {

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private GiftRepository giftRepository;
    @Autowired private DataDictRepository dataDictRepository;
    @Autowired private GiftLogRepository giftLogRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private String uid() { return UUID.randomUUID().toString().substring(0, 8); }

    private TransactionTemplate newTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }

    @Test
    @DisplayName("giftLevel=0 -> INVITE_NEW_1 GiftLog")
    void levelZero() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Product p = productRepository.save(Product.builder()
                    .name("p").code("rp_" + u).currentStock(100).safeStock(10)
                    .unit("pcs").price(BigDecimal.TEN).status("1").build());
            giftRepository.save(Gift.builder().name("L1").code("L1_" + u)
                    .type(Gift.GiftType.NEW).giftStatus(Gift.GiftStatus.ACTIVE)
                    .status("ACTIVE").product(p).build());
            dataDictRepository.save(DataDict.builder()
                    .groupCode("INVITE_NEW").groupName("INV")
                    .paramCode("INVITE_NEW_1").paramName("l1").paramValue("L1_" + u)
                    .build());
            Customer c = customerRepository.save(Customer.builder()
                    .name("rL0").phone("ph_" + u).giftLevel(0)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new ReferrerGiftEvent(c));
        });
        // 在事务内查询，避免 Gift 懒加载异常
        newTx().executeWithoutResult(s -> {
            var page = giftLogRepository.findByCustomerId(cid[0], PageRequest.of(0, 10));
            assertThat(page).isNotEmpty();
            assertThat(page.getContent().get(0).getGiftLogStatus()).isEqualTo(GiftLog.GiftLogStatus.PENDING);
            assertThat(page.getContent().get(0).getGift().getCode()).isEqualTo("L1_" + u);
        });
    }

    @Test
    @DisplayName("giftLevel=null -> no dict found, no GiftLog")
    void levelNull() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Customer c = customerRepository.save(Customer.builder()
                    .name("rLN").phone("ph_" + u).giftLevel(null)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new ReferrerGiftEvent(c));
        });
        assertThat(giftLogRepository.findByCustomerId(cid[0], PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    @DisplayName("giftLevel=1 -> INVITE_NEW_2 GiftLog")
    void levelOne() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Product p = productRepository.save(Product.builder()
                    .name("p").code("rp_" + u).currentStock(100).safeStock(10)
                    .unit("pcs").price(BigDecimal.TEN).status("1").build());
            giftRepository.save(Gift.builder().name("L2").code("L2_" + u)
                    .type(Gift.GiftType.NEW).giftStatus(Gift.GiftStatus.ACTIVE)
                    .status("ACTIVE").product(p).build());
            dataDictRepository.save(DataDict.builder()
                    .groupCode("INVITE_NEW").groupName("INV")
                    .paramCode("INVITE_NEW_2").paramName("l2").paramValue("L2_" + u)
                    .build());
            Customer c = customerRepository.save(Customer.builder()
                    .name("rL1").phone("ph_" + u).giftLevel(1)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new ReferrerGiftEvent(c));
        });
        newTx().executeWithoutResult(s -> {
            var page = giftLogRepository.findByCustomerId(cid[0], PageRequest.of(0, 10));
            assertThat(page).isNotEmpty();
            assertThat(page.getContent().get(0).getGift().getCode()).isEqualTo("L2_" + u);
        });
    }

    @Test
    @DisplayName("giftLevel=3 no GiftLog")
    void levelMax() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Customer c = customerRepository.save(Customer.builder()
                    .name("rL3").phone("ph_" + u).giftLevel(3)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new ReferrerGiftEvent(c));
        });
        assertThat(giftLogRepository.findByCustomerId(cid[0], PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    @DisplayName("giftLevel=4 no GiftLog")
    void levelAboveMax() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Customer c = customerRepository.save(Customer.builder()
                    .name("rL4").phone("ph_" + u).giftLevel(4)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new ReferrerGiftEvent(c));
        });
        assertThat(giftLogRepository.findByCustomerId(cid[0], PageRequest.of(0, 10))).isEmpty();
    }

    @Test
    @DisplayName("giftLevel=2 no dict, no GiftLog")
    void levelTwoNoDict() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Customer c = customerRepository.save(Customer.builder()
                    .name("rL2").phone("ph_" + u).giftLevel(2)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new ReferrerGiftEvent(c));
        });
        assertThat(giftLogRepository.findByCustomerId(cid[0], PageRequest.of(0, 10))).isEmpty();
    }
}
