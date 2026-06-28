package com.henashi.inventorycrm.listener;

import com.henashi.inventorycrm.event.GiftLogIssueEvent;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.pojo.GiftLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.GiftRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GiftLogIssueEventListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private GiftRepository giftRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private String uid() { return UUID.randomUUID().toString().substring(0, 8); }

    private TransactionTemplate newTx() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }

    @Test
    @DisplayName("giftLevel=0 to 1")
    void levelZeroToOne() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Product p = productRepository.save(Product.builder()
                    .name("p").code("p_" + u).currentStock(100).safeStock(10)
                    .unit("pcs").price(BigDecimal.TEN).status("1").build());
            Gift g = giftRepository.save(Gift.builder()
                    .name("g").code("g_" + u).type(Gift.GiftType.NEW)
                    .giftStatus(Gift.GiftStatus.ACTIVE).status("ACTIVE").product(p).build());
            Customer c = customerRepository.save(Customer.builder()
                    .name("L0").phone("ph_" + u).giftLevel(0)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new GiftLogIssueEvent(
                    GiftLog.builder().customer(c).gift(g)
                            .giftLogStatus(GiftLog.GiftLogStatus.ISSUED)
                            .quantity(1).operator("t").build()));
        });
        assertThat(customerRepository.findById(cid[0]).orElseThrow().getGiftLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("giftLevel=1 to 2")
    void levelOneToTwo() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Product p = productRepository.save(Product.builder()
                    .name("p").code("p_" + u).currentStock(100).safeStock(10)
                    .unit("pcs").price(BigDecimal.TEN).status("1").build());
            Gift g = giftRepository.save(Gift.builder()
                    .name("g").code("g_" + u).type(Gift.GiftType.NEW)
                    .giftStatus(Gift.GiftStatus.ACTIVE).status("ACTIVE").product(p).build());
            Customer c = customerRepository.save(Customer.builder()
                    .name("L1").phone("ph_" + u).giftLevel(1)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new GiftLogIssueEvent(
                    GiftLog.builder().customer(c).gift(g)
                            .giftLogStatus(GiftLog.GiftLogStatus.ISSUED)
                            .quantity(1).operator("t").build()));
        });
        assertThat(customerRepository.findById(cid[0]).orElseThrow().getGiftLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("giftLevel=2 to 3")
    void levelTwoToThree() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Product p = productRepository.save(Product.builder()
                    .name("p").code("p_" + u).currentStock(100).safeStock(10)
                    .unit("pcs").price(BigDecimal.TEN).status("1").build());
            Gift g = giftRepository.save(Gift.builder()
                    .name("g").code("g_" + u).type(Gift.GiftType.NEW)
                    .giftStatus(Gift.GiftStatus.ACTIVE).status("ACTIVE").product(p).build());
            Customer c = customerRepository.save(Customer.builder()
                    .name("L2").phone("ph_" + u).giftLevel(2)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new GiftLogIssueEvent(
                    GiftLog.builder().customer(c).gift(g)
                            .giftLogStatus(GiftLog.GiftLogStatus.ISSUED)
                            .quantity(1).operator("t").build()));
        });
        assertThat(customerRepository.findById(cid[0]).orElseThrow().getGiftLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("giftLevel=null to 1")
    void levelNullToOne() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Product p = productRepository.save(Product.builder()
                    .name("p").code("p_" + u).currentStock(100).safeStock(10)
                    .unit("pcs").price(BigDecimal.TEN).status("1").build());
            Gift g = giftRepository.save(Gift.builder()
                    .name("g").code("g_" + u).type(Gift.GiftType.NEW)
                    .giftStatus(Gift.GiftStatus.ACTIVE).status("ACTIVE").product(p).build());
            Customer c = customerRepository.save(Customer.builder()
                    .name("LN").phone("ph_" + u).giftLevel(null)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new GiftLogIssueEvent(
                    GiftLog.builder().customer(c).gift(g)
                            .giftLogStatus(GiftLog.GiftLogStatus.ISSUED)
                            .quantity(1).operator("t").build()));
        });
        assertThat(customerRepository.findById(cid[0]).orElseThrow().getGiftLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("giftLevel=3 unchanged")
    void levelThreeNoChange() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Product p = productRepository.save(Product.builder()
                    .name("p").code("p_" + u).currentStock(100).safeStock(10)
                    .unit("pcs").price(BigDecimal.TEN).status("1").build());
            Gift g = giftRepository.save(Gift.builder()
                    .name("g").code("g_" + u).type(Gift.GiftType.NEW)
                    .giftStatus(Gift.GiftStatus.ACTIVE).status("ACTIVE").product(p).build());
            Customer c = customerRepository.save(Customer.builder()
                    .name("L3").phone("ph_" + u).giftLevel(3)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new GiftLogIssueEvent(
                    GiftLog.builder().customer(c).gift(g)
                            .giftLogStatus(GiftLog.GiftLogStatus.ISSUED)
                            .quantity(1).operator("t").build()));
        });
        assertThat(customerRepository.findById(cid[0]).orElseThrow().getGiftLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("giftLevel=4 unchanged")
    void levelFourNoChange() {
        TransactionTemplate tx = newTx();
        String u = uid();
        long[] cid = new long[1];
        tx.executeWithoutResult(s -> {
            Product p = productRepository.save(Product.builder()
                    .name("p").code("p_" + u).currentStock(100).safeStock(10)
                    .unit("pcs").price(BigDecimal.TEN).status("1").build());
            Gift g = giftRepository.save(Gift.builder()
                    .name("g").code("g_" + u).type(Gift.GiftType.NEW)
                    .giftStatus(Gift.GiftStatus.ACTIVE).status("ACTIVE").product(p).build());
            Customer c = customerRepository.save(Customer.builder()
                    .name("L4").phone("ph_" + u).giftLevel(4)
                    .registeredAt(LocalDate.now().minusMonths(1)).status("1").build());
            cid[0] = c.getId();
            eventPublisher.publishEvent(new GiftLogIssueEvent(
                    GiftLog.builder().customer(c).gift(g)
                            .giftLogStatus(GiftLog.GiftLogStatus.ISSUED)
                            .quantity(1).operator("t").build()));
        });
        assertThat(customerRepository.findById(cid[0]).orElseThrow().getGiftLevel()).isEqualTo(4);
    }
}
