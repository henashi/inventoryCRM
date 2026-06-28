package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("findDistinctCategories — 返回去重分类列表")
    void findDistinctCategories() {
        // given: 创建几个商品
        Product p1 = new Product();
        p1.setName("商品A");
        p1.setCode("CAT_A_001");
        p1.setCategory("电子产品");
        p1.setPrice(java.math.BigDecimal.valueOf(100));
        productRepository.save(p1);

        Product p2 = new Product();
        p2.setName("商品B");
        p2.setCode("CAT_A_002");
        p2.setCategory("电子产品");
        p2.setPrice(java.math.BigDecimal.valueOf(200));
        productRepository.save(p2);

        Product p3 = new Product();
        p3.setName("商品C");
        p3.setCode("CAT_B_001");
        p3.setCategory("食品饮料");
        p3.setPrice(java.math.BigDecimal.valueOf(50));
        productRepository.save(p3);

        // when
        List<String> categories = productRepository.findDistinctCategories();

        // then
        assertNotNull(categories);
        assertTrue(categories.contains("电子产品"));
        assertTrue(categories.contains("食品饮料"));
        assertEquals(2, categories.size(), "应去重返回 2 个分类");
    }
}
