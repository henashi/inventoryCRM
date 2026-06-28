package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.ImportResultDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProductDataExchangeServiceTest {

    @Autowired
    private ProductDataExchangeService productDataExchangeService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
    }

    // ==================== Import Template Meta ====================

    @Test
    @DisplayName("获取导入模板元数据 — 返回模板字段和说明")
    void getImportTemplateMeta() {
        ImportResultDTO meta = productDataExchangeService.getImportTemplateMeta();
        assertThat(meta.successCount()).isZero();
        assertThat(meta.failureCount()).isZero();
        assertThat(meta.templateFields()).contains("name", "code", "category", "price");
        assertThat(meta.requiredFields()).contains("name", "category", "price");
        assertThat(meta.notes()).isNotEmpty();
    }

    // ==================== Import Products — Success ====================

    @Test
    @DisplayName("导入商品 — CSV 数据导入成功")
    void importProductsSuccess() {
        String csv = "name,code,category,price,cost,currentStock,safeStock,unit,description,remark\n" +
                "新商品A,NEW_A,电子产品,99.90,50.00,200,20,台,测试商品A,\n" +
                "新商品B,NEW_B,食品,29.90,15.00,500,50,袋,测试商品B,无备注\n";

        MockMultipartFile file = new MockMultipartFile("file", "products.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = productDataExchangeService.importProducts(file);

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isZero();
        assertThat(result.failureDetails()).isEmpty();
    }

    @Test
    @DisplayName("导入商品 — 自动生成编码（code 为空）")
    void importProductsWithEmptyCode() {
        String csv = "name,code,category,price,cost,currentStock,safeStock,unit\n" +
                "无编码商品,,日用品,10.00,5.00,100,10,个\n";

        MockMultipartFile file = new MockMultipartFile("file", "products.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = productDataExchangeService.importProducts(file);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();
    }

    // ==================== Import Products — Validation errors ====================

    @Test
    @DisplayName("导入商品 — 空名称跳过")
    void importProductsEmptyName() {
        String csv = "name,code,category,price,cost,currentStock,safeStock,unit\n" +
                ",,电子产品,99.90,50.00,200,20,台\n";

        MockMultipartFile file = new MockMultipartFile("file", "products.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = productDataExchangeService.importProducts(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("导入商品 — 空分类跳过")
    void importProductsEmptyCategory() {
        String csv = "name,code,category,price,cost,currentStock,safeStock,unit\n" +
                "测试商品,,,99.90,50.00,200,20,个\n";

        MockMultipartFile file = new MockMultipartFile("file", "products.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = productDataExchangeService.importProducts(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("导入商品 — 编码重复（数据库已存在）跳过")
    void importProductsCodeExists() {
        String csv = "name,code,category,price,cost,currentStock,safeStock,unit\n" +
                "重复编码商品,PRO_TEST_001,测试,99.90,50.00,200,20,个\n";

        MockMultipartFile file = new MockMultipartFile("file", "products.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = productDataExchangeService.importProducts(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureDetails().get(0).reason()).contains("编码已存在");
    }

    @Test
    @DisplayName("导入商品 — 文件内编码重复跳过")
    void importProductsCodeDuplicateInFile() {
        String csv = "name,code,category,price,cost,currentStock,safeStock,unit\n" +
                "商品A,DUP_CODE,分类,10.00,5.00,100,10,个\n" +
                "商品B,DUP_CODE,分类,20.00,10.00,200,20,个\n";

        MockMultipartFile file = new MockMultipartFile("file", "products.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = productDataExchangeService.importProducts(file);

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureDetails().get(0).reason()).contains("编码重复");
    }

    @Test
    @DisplayName("导入商品 — 当前库存为负数跳过")
    void importProductsNegativeStock() {
        String csv = "name,code,category,price,cost,currentStock,safeStock,unit\n" +
                "负库存商品,NEG_STK,分类,10.00,5.00,-1,10,个\n";

        MockMultipartFile file = new MockMultipartFile("file", "products.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = productDataExchangeService.importProducts(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureDetails().get(0).reason()).contains("库存不能为负数");
    }

    // ==================== Import Products — File validation ====================

    @Test
    @DisplayName("导入商品 — 空文件抛出异常")
    void importProductsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv",
                "text/csv", new byte[0]);

        assertThrows(BusinessException.class,
                () -> productDataExchangeService.importProducts(file));
    }

    @Test
    @DisplayName("导入商品 — 非 CSV 文件抛出异常")
    void importProductsWrongFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "data.json",
                "application/json", "{}".getBytes(StandardCharsets.UTF_8));

        assertThrows(BusinessException.class,
                () -> productDataExchangeService.importProducts(file));
    }

    @Test
    @DisplayName("导入商品 — 缺少必填列表头抛出异常")
    void importProductsMissingRequiredHeaders() {
        String csv = "name,code\n测试商品,NEW_CODE\n";
        MockMultipartFile file = new MockMultipartFile("file", "products.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThrows(BusinessException.class,
                () -> productDataExchangeService.importProducts(file));
    }
}
