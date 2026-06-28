package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.OrderItemCreateDTO;
import com.henashi.inventorycrm.dto.OrderItemDTO;
import com.henashi.inventorycrm.exception.BusinessException;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class OrderItemServiceTest {

    @Autowired
    private OrderItemService orderItemService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
    }

    // ==================== Create ====================

    @Test
    @DisplayName("创建消费记录 — 关联商品")
    void createWithProduct() {
        OrderItemCreateDTO dto = new OrderItemCreateDTO(
                1L,              // customerId
                1L,              // productId
                null,            // productName
                2,               // quantity
                BigDecimal.valueOf(50), // unitPrice
                BigDecimal.valueOf(100), // totalAmount
                "测试备注"
        );

        OrderItemDTO result = orderItemService.create(dto);

        assertThat(result.id()).isNotNull();
        assertThat(result.customerId()).isEqualTo(1L);
        assertThat(result.customerName()).isEqualTo("测试客户");
        assertThat(result.productId()).isEqualTo(1L);
        assertThat(result.productName()).isEqualTo("测试商品");
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("创建消费记录 — 不关联商品（自定义商品名）")
    void createWithoutProduct() {
        OrderItemCreateDTO dto = new OrderItemCreateDTO(
                1L,              // customerId
                null,            // productId
                "自定义服务费",     // productName
                1,               // quantity
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(200),
                null
        );

        OrderItemDTO result = orderItemService.create(dto);

        assertThat(result.id()).isNotNull();
        assertThat(result.productId()).isNull();
        assertThat(result.productName()).isEqualTo("自定义服务费");
        assertThat(result.totalAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    @DisplayName("创建消费记录 — 不存在的客户抛出异常")
    void createCustomerNotFound() {
        OrderItemCreateDTO dto = new OrderItemCreateDTO(
                99999L, 1L, null, 1,
                BigDecimal.TEN, BigDecimal.TEN, null
        );

        assertThrows(BusinessException.class, () -> orderItemService.create(dto));
    }

    // ==================== Batch Create ====================

    @Test
    @DisplayName("批量创建消费记录")
    void batchCreate() {
        OrderItemCreateDTO dto1 = new OrderItemCreateDTO(
                1L, 1L, null, 1, BigDecimal.valueOf(50), BigDecimal.valueOf(50), null
        );
        OrderItemCreateDTO dto2 = new OrderItemCreateDTO(
                1L, null, "手工服务", 3, BigDecimal.valueOf(30), BigDecimal.valueOf(90), null
        );

        List<OrderItemDTO> results = orderItemService.batchCreate(List.of(dto1, dto2));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).productName()).isEqualTo("测试商品");
        assertThat(results.get(1).productName()).isEqualTo("手工服务");
    }

    // ==================== List ====================

    @Test
    @DisplayName("分页列表 — 空数据返回空分页")
    void listEmpty() {
        Page<OrderItemDTO> page = orderItemService.list(PageRequest.of(0, 10));
        assertThat(page).isNotNull();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("分页列表 — 创建后查询到数据")
    void listAfterCreate() {
        OrderItemCreateDTO dto = new OrderItemCreateDTO(
                1L, 1L, null, 2, BigDecimal.valueOf(50), BigDecimal.valueOf(100), null
        );
        orderItemService.create(dto);

        Page<OrderItemDTO> page = orderItemService.list(PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
    }

    // ==================== List by Customer ====================

    @Test
    @DisplayName("按客户查询 — 创建后返回该客户记录")
    void listByCustomer() {
        OrderItemCreateDTO dto = new OrderItemCreateDTO(
                1L, 1L, null, 1, BigDecimal.valueOf(50), BigDecimal.valueOf(50), null
        );
        orderItemService.create(dto);

        Page<OrderItemDTO> page = orderItemService.listByCustomer(1L, PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().get(0).customerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("按客户查询 — 不存在的客户返回空")
    void listByCustomerNotFound() {
        Page<OrderItemDTO> page = orderItemService.listByCustomer(99999L, PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }

    // ==================== Get By Id ====================

    @Test
    @DisplayName("按 ID 查询 — 不存在的记录抛出异常")
    void getByIdNotFound() {
        assertThrows(BusinessException.class, () -> orderItemService.getById(99999L));
    }

    @Test
    @DisplayName("按 ID 查询 — 创建后可以查到")
    void getByIdAfterCreate() {
        OrderItemCreateDTO dto = new OrderItemCreateDTO(
                1L, 1L, null, 1, BigDecimal.valueOf(50), BigDecimal.valueOf(50), null
        );
        OrderItemDTO created = orderItemService.create(dto);

        OrderItemDTO found = orderItemService.getById(created.id());
        assertThat(found.id()).isEqualTo(created.id());
    }

    // ==================== Delete ====================

    @Test
    @DisplayName("软删除 — 删除后查询不到")
    void delete() {
        OrderItemCreateDTO dto = new OrderItemCreateDTO(
                1L, 1L, null, 1, BigDecimal.valueOf(50), BigDecimal.valueOf(50), null
        );
        OrderItemDTO created = orderItemService.create(dto);
        Long id = created.id();

        orderItemService.delete(id);
        assertThrows(BusinessException.class, () -> orderItemService.getById(id));
    }
}
