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
class CustomerDataExchangeServiceTest {

    @Autowired
    private CustomerDataExchangeService customerDataExchangeService;

    @Autowired
    private CustomerService customerService;

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
        ImportResultDTO meta = customerDataExchangeService.getImportTemplateMeta();
        assertThat(meta.successCount()).isZero();
        assertThat(meta.failureCount()).isZero();
        assertThat(meta.templateFields()).contains("name", "phone", "email", "address");
        assertThat(meta.requiredFields()).contains("name", "phone");
        assertThat(meta.notes()).isNotEmpty();
    }

    // ==================== Import Customers — Simple CSV ====================

    @Test
    @DisplayName("导入客户 — 最小 CSV name+phone")
    void importCustomersMinimal() {
        String csv = "name,phone\n测试客户A,13900000001\n";
        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);
        assertThat(result.successCount()).as("expect 1 success").isEqualTo(1);
        assertThat(result.failureCount()).as("expect 0 failures").isZero();
    }

    @Test
    @DisplayName("导入客户 — CSV 含全部字段（表头全小写）")
    void importCustomersSuccess() {
        // 表头必须全小写！CsvUtils.normalizeHeader 会做 toLowerCase()
        String csv = "name,phone,email,address,birthday,gender,giftLevel,remark\n" +
                "张三,13900000001,zhangsan@test.com,北京,1990-01-01,1,1,测试导入\n" +
                "李四,13900000002,lisi@test.com,上海,1995-05-05,0,2,\n";

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        if (result.failureCount() > 0) {
            StringBuilder sb = new StringBuilder("Import failures:\n");
            result.failureDetails().forEach(fd ->
                sb.append("  Row ").append(fd.rowNumber()).append(" (").append(fd.identifier()).append("): ").append(fd.reason()).append("\n"));
            System.err.println(sb.toString());
        }
        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isZero();
        assertThat(result.failureDetails()).isEmpty();
    }

    // ==================== Import Customers — Validation errors ====================

    @Test
    @DisplayName("导入客户 — 空名称行跳过并记录失败")
    void importCustomersEmptyName() {
        String csv = "name,phone\n" +
                ",13900000010\n";

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureDetails().get(0).reason()).contains("姓名不能为空");
    }

    @Test
    @DisplayName("导入客户 — 空手机号行跳过并记录失败")
    void importCustomersEmptyPhone() {
        String csv = "name,phone\n" +
                "王五,\n";

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureDetails().get(0).reason()).contains("手机号");
    }

    @Test
    @DisplayName("导入客户 — 手机号格式不正确记录失败")
    void importCustomersInvalidPhone() {
        String csv = "name,phone\n" +
                "赵六,12345\n";

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureDetails().get(0).reason()).contains("手机号格式不正确");
    }

    @Test
    @DisplayName("导入客户 — 文件内手机号重复跳过")
    void importCustomersDuplicatePhoneInFile() {
        String csv = "name,phone\n" +
                "钱七,13900000020\n" +
                "孙八,13900000020\n";

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        if (result.failureCount() > 0) {
            StringBuilder sb = new StringBuilder("Import failures:\n");
            result.failureDetails().forEach(fd ->
                sb.append("  Row ").append(fd.rowNumber()).append(" (").append(fd.identifier()).append("): ").append(fd.reason()).append("\n"));
            System.err.println(sb.toString());
        }
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureDetails().get(0).reason()).contains("手机号重复");
    }

    @Test
    @DisplayName("导入客户 — 数据库中已存在的手机号跳过")
    void importCustomersPhoneAlreadyExists() {
        String csv = "name,phone\n" +
                "周九,13800000000\n"; // data.sql 中存在的手机号

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
    }

    // ==================== Import Customers — File validation ====================

    @Test
    @DisplayName("导入客户 — 空文件抛出异常")
    void importCustomersEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv",
                "text/csv", new byte[0]);

        assertThrows(BusinessException.class,
                () -> customerDataExchangeService.importCustomers(file));
    }

    @Test
    @DisplayName("导入客户 — 非 CSV 文件抛出异常")
    void importCustomersWrongFileType() {
        MockMultipartFile file = new MockMultipartFile("file", "data.txt",
                "text/plain", "name,phone\n测试,13900000099".getBytes(StandardCharsets.UTF_8));

        assertThrows(BusinessException.class,
                () -> customerDataExchangeService.importCustomers(file));
    }

    @Test
    @DisplayName("导入客户 — 缺少必填列表头抛出异常")
    void importCustomersMissingRequiredHeaders() {
        String csv = "name,email\n张三,zhangsan@test.com\n";
        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThrows(BusinessException.class,
                () -> customerDataExchangeService.importCustomers(file));
    }

    @Test
    @DisplayName("导入客户 — 邮箱格式不正确记录失败")
    void importCustomersInvalidEmail() {
        String csv = "name,phone,email\n" +
                "吴十,13900000030,not-an-email\n";

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("导入客户 — 性别值不合法记录失败")
    void importCustomersInvalidGender() {
        String csv = "name,phone,gender\n" +
                "郑一,13900000040,3\n";

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("导入客户 — 礼品等级超出范围记录失败（表头小写）")
    void importCustomersInvalidGiftLevel() {
        String csv = "name,phone,giftLevel\n" +
                "冯二,13900000050,5\n";

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("导入客户 — 推荐人手机号与客户相同记录失败")
    void importCustomersReferrerSameAsCustomer() {
        String csv = "name,phone,referrerPhone\n" +
                "陈三,13900000060,13900000060\n";

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        if (result.failureCount() > 0) {
            StringBuilder sb = new StringBuilder("Import failures:\n");
            result.failureDetails().forEach(fd ->
                sb.append("  Row ").append(fd.rowNumber()).append(" (").append(fd.identifier()).append("): ").append(fd.reason()).append("\n"));
            System.err.println(sb.toString());
        }
        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureDetails().get(0).reason()).contains("推荐人手机号不能与客户手机号相同");
    }

    @Test
    @DisplayName("导入客户 — 推荐人手机号不存在记录失败")
    void importCustomersReferrerNotFound() {
        String csv = "name,phone,referrerPhone\n" +
                "何四,13900000070,13900000999\n";

        MockMultipartFile file = new MockMultipartFile("file", "customers.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultDTO result = customerDataExchangeService.importCustomers(file);

        if (result.failureCount() > 0) {
            StringBuilder sb = new StringBuilder("Import failures:\n");
            result.failureDetails().forEach(fd ->
                sb.append("  Row ").append(fd.rowNumber()).append(" (").append(fd.identifier()).append("): ").append(fd.reason()).append("\n"));
            System.err.println(sb.toString());
        }
        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureDetails().get(0).reason()).contains("推荐人手机号不存在");
    }
}
