package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CustomerOrderServiceTest {

    @Autowired private CustomerOrderService orderService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test @DisplayName("创建订单")
    void createOrder() {
        OrderItemCreateDTO item = new OrderItemCreateDTO(1L, 1L, "测试商品", 2,
                BigDecimal.valueOf(50), BigDecimal.valueOf(100), null);
        OrderCreateDTO dto = new OrderCreateDTO(1L, BigDecimal.ZERO, "测试订单", List.of(item));
        OrderDTO r = orderService.create(dto);
        assertThat(r.id()).isNotNull();
    }

    @Test @DisplayName("查询 — 分页")
    void listOrders() {
        var page = orderService.list(PageRequest.of(0, 10));
        assertThat(page).isNotNull();
    }

    @Test @DisplayName("查询 — 详情")
    void findById() {
        // 先创建再查
        OrderItemCreateDTO item = new OrderItemCreateDTO(1L, 1L, "测试商品", 2,
                BigDecimal.valueOf(50), BigDecimal.valueOf(100), null);
        OrderCreateDTO dto = new OrderCreateDTO(1L, BigDecimal.ZERO, "测试", List.of(item));
        OrderDTO created = orderService.create(dto);
        OrderDTO r = orderService.getById(created.id());
        assertThat(r).isNotNull();
    }

    @Test @DisplayName("删除订单")
    void deleteOrder() {
        OrderItemCreateDTO item = new OrderItemCreateDTO(1L, 1L, "测试商品", 2,
                BigDecimal.valueOf(50), BigDecimal.valueOf(100), null);
        OrderCreateDTO dto = new OrderCreateDTO(1L, BigDecimal.ZERO, "测试", List.of(item));
        OrderDTO created = orderService.create(dto);
        orderService.delete(created.id());
        assertThrows(RuntimeException.class, () -> orderService.getById(created.id()));
    }

    @Test @DisplayName("创建订单 — 折扣超过总金额时 finalAmount 为 0")
    void createOrderDiscountExceedsTotal() {
        OrderItemCreateDTO item = new OrderItemCreateDTO(1L, 1L, "测试商品", 1,
                BigDecimal.valueOf(50), BigDecimal.valueOf(100), null);
        // 折扣 200 > 总金额 100
        OrderCreateDTO dto = new OrderCreateDTO(1L, BigDecimal.valueOf(200), "高折扣", List.of(item));
        OrderDTO r = orderService.create(dto);
        assertThat(r.finalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("创建订单 — 多商品总金额合计正确")
    void createOrderMultipleItemsTotal() {
        OrderItemCreateDTO item1 = new OrderItemCreateDTO(1L, 1L, "商品A", 2,
                BigDecimal.valueOf(50), BigDecimal.valueOf(100), null);
        OrderItemCreateDTO item2 = new OrderItemCreateDTO(1L, 1L, "商品B", 1,
                BigDecimal.valueOf(80), BigDecimal.valueOf(80), null);
        OrderCreateDTO dto = new OrderCreateDTO(1L, BigDecimal.TEN, "多商品", List.of(item1, item2));
        OrderDTO r = orderService.create(dto);
        // totalAmount = 100 + 80 = 180, discount = 10, finalAmount = 170
        assertThat(r.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(180));
        assertThat(r.finalAmount()).isEqualByComparingTo(BigDecimal.valueOf(170));
    }

    @Test @DisplayName("创建订单 — 客户不存在抛出异常")
    void createOrderCustomerNotFound() {
        OrderItemCreateDTO item = new OrderItemCreateDTO(999L, 1L, "测试商品", 1,
                BigDecimal.valueOf(50), BigDecimal.valueOf(50), null);
        OrderCreateDTO dto = new OrderCreateDTO(999L, BigDecimal.ZERO, "不存在的客户", List.of(item));
        assertThrows(RuntimeException.class, () -> orderService.create(dto));
    }

    @Test @DisplayName("创建订单 — 零金额订单")
    void createOrderZeroAmount() {
        OrderItemCreateDTO item = new OrderItemCreateDTO(1L, 1L, "测试商品", 1,
                BigDecimal.ZERO, BigDecimal.ZERO, null);
        OrderCreateDTO dto = new OrderCreateDTO(1L, BigDecimal.ZERO, "零金额", List.of(item));
        OrderDTO r = orderService.create(dto);
        assertThat(r.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.finalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("创建订单 — 折扣为 null 时默认视为 0")
    void createOrderNullDiscount() {
        OrderItemCreateDTO item = new OrderItemCreateDTO(1L, 1L, "测试商品", 2,
                BigDecimal.valueOf(50), BigDecimal.valueOf(100), null);
        OrderCreateDTO dto = new OrderCreateDTO(1L, null, "无折扣参数", List.of(item));
        OrderDTO r = orderService.create(dto);
        // finalAmount 应等于 totalAmount 而非 NPE
        assertThat(r.finalAmount()).isEqualByComparingTo(r.totalAmount());
    }

    private void assertThrows(Class<RuntimeException> clazz, Runnable r) {
        try { r.run(); throw new AssertionError("Expected exception"); }
        catch (RuntimeException e) { assertThat(e).isInstanceOf(clazz); }
    }
}
