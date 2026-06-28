package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.*;
import com.henashi.inventorycrm.exception.CustomerException;
import com.henashi.inventorycrm.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 客户服务集成测试
 */
@SpringBootTest
@Transactional
class CustomerServiceTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
    }

    @Test
    @DisplayName("创建客户 — 基本字段持久化")
    void createCustomer() {
        CustomerCreateDTO dto = new CustomerCreateDTO(
                "张三", "13900000001", "北京市", "z@test.com",
                1, LocalDate.now(), null, 1, null, "测试", 1, 1, LocalDate.of(1990, 1, 1));
        CustomerDTO c = customerService.createCustomer(dto);
        assertThat(c.id()).isNotNull();
        assertThat(c.name()).isEqualTo("张三");
    }

    @Test
    @DisplayName("创建客户 — 手机号重复抛出 CustomerException")
    void createCustomerDuplicatePhone() {
        CustomerCreateDTO dto = new CustomerCreateDTO(
                "重复", "13800000000", null, null, null, null, null, null, null, null, null, null, null);
        assertThrows(CustomerException.class, () -> customerService.createCustomer(dto));
    }

    @Test
    @DisplayName("查询 — 根据 ID 获取客户")
    void findById() {
        CustomerDTO c = customerService.findCustomerDTOById(1L);
        assertThat(c.name()).isEqualTo("测试客户");
    }

    @Test
    @DisplayName("分页查询 — 列表含数据")
    void findAllCustomers() {
        Page<CustomerDTO> page = customerService.findAllCustomers(
                PageRequest.of(0, 10), null, null, null, null, null, null, null, null);
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("更新客户 — 修改名称和手机号")
    void updateCustomer() {
        // CustomerUpdateDTO 有 14 个字段: id, name, phone, address, email, type,
        // registeredAt, referrerId, giftLevel, giftReceivedAt, remark, status, gender, birthday
        CustomerUpdateDTO dto = new CustomerUpdateDTO(
                null, "新名称", "13700000001", "新地址", "new@test.com",
                1, null, null, 2, null, "更新测试", 1, 1, LocalDate.of(1990, 1, 1));
        CustomerDTO updated = customerService.updateCustomer(1L, dto);
        assertThat(updated.name()).isEqualTo("新名称");
        assertThat(updated.phone()).isEqualTo("13700000001");
    }

    @Test
    @DisplayName("软删除 — 删除后查询抛出异常")
    void deleteCustomer() {
        customerService.deleteById(1L);
        assertThrows(RuntimeException.class, () -> customerService.findCustomerDTOById(1L));
    }

    @Test
    @DisplayName("批量更新状态 — 将客户状态设为 0")
    void batchUpdateStatus() {
        CustomerBatchStatusUpdateResultDTO r = customerService.batchUpdateStatus(
                new CustomerBatchStatusUpdateDTO(List.of(1L), 0));
        assertThat(r.success()).isTrue();
        assertThat(r.updatedCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("统计 — 返回总客户数")
    void getStatistics() {
        CustomerStatisticsDTO stats = customerService.getStatistics();
        assertThat(stats.totalCustomers()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("创建客户 — 可关联推荐人")
    void createCustomerWithReferrer() {
        CustomerCreateDTO refDto = new CustomerCreateDTO(
                "推荐人", "13600000001", null, null, null, null, null, null, null, null, null, null, null);
        CustomerDTO referrer = customerService.createCustomer(refDto);

        CustomerCreateDTO referredDto = new CustomerCreateDTO(
                "被推荐人", "13600000002", null, null, null, null, referrer.id(),
                1, null, "测试", 1, 1, null);
        CustomerDTO referred = customerService.createCustomer(referredDto);
        assertThat(referred.referrerName()).isEqualTo("推荐人");
    }
}
