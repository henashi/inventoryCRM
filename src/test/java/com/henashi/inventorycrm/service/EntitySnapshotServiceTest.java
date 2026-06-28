package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EntitySnapshotServiceTest {

    @Autowired
    private EntitySnapshotService entitySnapshotService;

    @Test
    @DisplayName("获取快照 — 按 ID 查询 Product 并脱管")
    void getOldDataProduct() {
        Product snapshot = entitySnapshotService.getOldDataInNewTransaction(Product.class, 1L);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getId()).isEqualTo(1L);
        assertThat(snapshot.getName()).isEqualTo("测试商品");
    }

    @Test
    @DisplayName("获取快照 — 按 ID 查询 Customer 并脱管")
    void getOldDataCustomer() {
        Customer snapshot = entitySnapshotService.getOldDataInNewTransaction(Customer.class, 1L);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getId()).isEqualTo(1L);
        assertThat(snapshot.getName()).isEqualTo("测试客户");
    }

    @Test
    @DisplayName("获取快照 — ID 为 null 返回 null")
    void getOldDataNullId() {
        Product snapshot = entitySnapshotService.getOldDataInNewTransaction(Product.class, null);
        assertThat(snapshot).isNull();
    }

    @Test
    @DisplayName("获取快照 — 不存在的 ID 返回 null")
    void getOldDataNonExistentId() {
        Customer snapshot = entitySnapshotService.getOldDataInNewTransaction(Customer.class, 99999L);
        assertThat(snapshot).isNull();
    }
}
