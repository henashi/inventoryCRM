package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.*;
import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProductServiceTest {

    @Autowired private ProductService productService;
    @Autowired private ProductRepository productRepository;

    private Product product;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        product = productRepository.findById(1L).orElseThrow();
        product.setCurrentStock(100);
        product.setSafeStock(10);
        productRepository.save(product);
    }

    @Test @DisplayName("创建商品")
    void createProduct() {
        ProductCreateDTO dto = new ProductCreateDTO("新商品", "NEW001", null, 100, 10, "个",
                BigDecimal.TEN, null, null, null);
        ProductDTO r = productService.saveProduct(dto);
        assertThat(r.id()).isNotNull();
        assertThat(r.name()).isEqualTo("新商品");
    }

    @Test @DisplayName("创建商品 — 编码重复抛异常")
    void createProductDuplicateCode() {
        ProductCreateDTO dto = new ProductCreateDTO("重复", "PRO_TEST_001", null, 50, 5, "个",
                BigDecimal.TEN, null, null, null);
        assertThrows(RuntimeException.class, () -> productService.saveProduct(dto));
    }

    @Test @DisplayName("查询 — 根据 ID")
    void findById() {
        ProductDTO p = productService.findProductDTOById(1L);
        assertThat(p.name()).isEqualTo("测试商品");
    }

    @Test @DisplayName("更新商品")
    void updateProduct() {
        ProductUpdateDTO dto = new ProductUpdateDTO("新名称", "NEW_CODE", "箱", 200,
                20, "箱", BigDecimal.valueOf(20), null, "更新备注", null, 1);
        ProductDTO r = productService.updateProduct(1L, dto);
        assertThat(r.name()).isEqualTo("新名称");
    }

    @Test @DisplayName("软删除")
    void deleteProduct() {
        productService.deleteById(1L);
        assertThrows(RuntimeException.class, () -> productService.findProductDTOById(1L));
    }

    @Test @DisplayName("入库 — 增加库存")
    void stockIn() {
        Product r = productService.changeStock(product, InventoryLog.LogType.IN, 30);
        assertThat(r.getCurrentStock()).isEqualTo(130);
    }

    @Test @DisplayName("出库 — 扣减库存")
    void stockOut() {
        Product r = productService.changeStock(product, InventoryLog.LogType.OUT, 20);
        assertThat(r.getCurrentStock()).isEqualTo(80);
    }

    @Test @DisplayName("调整库存")
    void adjustStock() {
        Product r = productService.adjustStock(product, 66);
        assertThat(r.getCurrentStock()).isEqualTo(66);
    }

    @Test @DisplayName("低库存查询")
    void findLowStock() {
        productService.adjustStock(product, 3);
        List<ProductDTO> list = productService.findLowStockProducts(null);
        assertThat(list).anyMatch(p -> p.id().equals(1L));
    }

    @Test @DisplayName("分类列表")
    void getCategories() {
        List<String> cats = productService.getCategories();
        assertThat(cats).isNotNull();
    }

    @Test @DisplayName("库存统计")
    void getStockStatistics() {
        ProductStockStatisticsDTO s = productService.getStockStatistics();
        assertThat(s.totalProducts()).isGreaterThanOrEqualTo(1);
    }

    @Test @DisplayName("创建商品 — 价格为负数抛出异常")
    void createProductNegativePrice() {
        ProductCreateDTO dto = new ProductCreateDTO("负价格", "NEG001", null, 100, 10, "个",
                BigDecimal.valueOf(-1), null, null, null);
        assertThrows(RuntimeException.class, () -> productService.saveProduct(dto));
    }

    @Test @DisplayName("更新商品 — 编码冲突抛出异常")
    void updateProductCodeConflict() {
        ProductCreateDTO newDto = new ProductCreateDTO("另一商品", "ANOTHER_CODE", null, 50, 5, "个",
                BigDecimal.TEN, null, null, null);
        ProductDTO created = productService.saveProduct(newDto);

        // 尝试将编码改为已存在的 PRO_TEST_001
        ProductUpdateDTO updateDto = new ProductUpdateDTO("另一商品", "PRO_TEST_001", null, 50,
                5, "个", BigDecimal.TEN, null, null, null, 1);
        assertThrows(RuntimeException.class, () -> productService.updateProduct(created.id(), updateDto));
    }

    @Test @DisplayName("变更库存 — 无效类型抛出异常")
    void changeStockInvalidType() {
        assertThrows(IllegalArgumentException.class, () ->
                productService.changeStock(product, InventoryLog.LogType.ADJUST, 10));
    }

    @Test @DisplayName("库存检查 — 库存充足通过")
    void checkStockSufficient() {
        productService.checkStock(1L, 50);
    }

    @Test @DisplayName("库存检查 — 库存不足抛出异常")
    void checkStockInsufficient() {
        assertThrows(RuntimeException.class, () -> productService.checkStock(1L, 999));
    }

    @Test @DisplayName("导出 — CSV 含 BOM 头")
    void exportProducts() {
        byte[] csv = productService.exportProducts(null, null, null);
        assertThat(csv).isNotEmpty();
        String content = new String(csv, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(content).startsWith("\uFEFF");
        assertThat(content).contains("名称");
        assertThat(content).contains("测试商品");
    }

    @Test @DisplayName("搜索 — 空关键词返回空列表")
    void searchProductsEmptyKeyword() {
        assertThat(productService.searchProducts(null, 10)).isEmpty();
        assertThat(productService.searchProducts("", 10)).isEmpty();
        assertThat(productService.searchProducts("   ", 10)).isEmpty();
    }

    @Test @DisplayName("低库存 — 负阈值抛出异常")
    void findLowStockProductsNegativeThreshold() {
        assertThrows(RuntimeException.class, () -> productService.findLowStockProducts(-1));
    }

    @Test @DisplayName("更新库存 — 字符串类型的入库/出库")
    void updateStockByStringType() {
        ProductDTO r = productService.updateStock(1L, "IN", 20, "字符串类型入库");
        assertThat(r.currentStock()).isEqualTo(120);

        ProductDTO r2 = productService.updateStock(1L, "OUT", 30, "字符串类型出库");
        assertThat(r2.currentStock()).isEqualTo(90);

        assertThrows(IllegalArgumentException.class, () ->
                productService.updateStock(1L, "INVALID", 10, "无效类型"));
    }
}
