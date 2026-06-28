package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.CustomerCreateDTO;
import com.henashi.inventorycrm.dto.CustomerDTO;
import com.henashi.inventorycrm.dto.CustomerUpdateDTO;
import com.henashi.inventorycrm.pojo.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CustomerMapper 集成测试。
 * 验证 qualifiedByName 引用解析、dateDefaultNow 降级、partialUpdate 空字段不覆盖。
 */
@SpringBootTest
@Transactional
class CustomerMapperTest {

    @Autowired
    private CustomerMapper customerMapper;

    @Test
    @DisplayName("fromEntity — 含推荐人的客户映射扁平化")
    void fromEntityWithReferrer() {
        Customer referrer = new Customer();
        referrer.setId(1L);
        referrer.setName("推荐人");

        Customer customer = new Customer();
        customer.setId(2L);
        customer.setName("测试客户");
        customer.setPhone("13800138001");
        customer.setReferrer(referrer);

        CustomerDTO dto = customerMapper.fromEntity(customer);
        assertThat(dto.referrerId()).isEqualTo(1L);
        assertThat(dto.referrerName()).isEqualTo("推荐人");
    }

    @Test
    @DisplayName("fromEntity — 无推荐人时 referrer 字段为 null")
    void fromEntityWithoutReferrer() {
        Customer customer = new Customer();
        customer.setId(3L);
        customer.setName("独立客户");

        CustomerDTO dto = customerMapper.fromEntity(customer);
        assertThat(dto.referrerId()).isNull();
        assertThat(dto.referrerName()).isNull();
    }

    @Test
    @DisplayName("createToEntity — CustomerCreateDTO 转实体含推荐人解析")
    void createToEntityWithReferrer() {
        CustomerCreateDTO dto = new CustomerCreateDTO(
                "新客户", "13900139000", null, null, 1,
                LocalDate.now(), 1L, 0, null, "测试", 1, 0, LocalDate.of(1990, 1, 1));

        Customer entity = customerMapper.createToEntity(dto);
        assertThat(entity.getName()).isEqualTo("新客户");
        assertThat(entity.getPhone()).isEqualTo("13900139000");
        // referrerId=1 应解析为 Customer 实体
        assertThat(entity.getReferrer()).isNotNull();
        assertThat(entity.getReferrer().getId()).isEqualTo(1L);
        assertThat(entity.getType()).isEqualTo(1);
    }

    @Test
    @DisplayName("createToEntity — registeredAt 为 null 时使用当天日期")
    void createToEntityDateDefaultNow() {
        CustomerCreateDTO dto = new CustomerCreateDTO(
                "日期客户", "13900139001", null, null, 1,
                null, null, 0, null, "测试", 1, 0, null);

        Customer entity = customerMapper.createToEntity(dto);
        assertThat(entity.getRegisteredAt()).isNotNull();
        assertThat(entity.getRegisteredAt()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("partialUpdate — 空字段不覆盖已有值")
    void partialUpdateIgnoresNullFields() {
        Customer existing = new Customer();
        existing.setId(5L);
        existing.setName("原名");
        existing.setPhone("13800000000");

        // 只更新 name，phone 等字段为 null — 不应被覆盖
        CustomerUpdateDTO updateDto = new CustomerUpdateDTO(
                5L, "新名", null, null, null,
                null, null, null, null, null, null, null, null, null);

        customerMapper.partialUpdate(updateDto, existing);
        assertThat(existing.getName()).isEqualTo("新名");
        assertThat(existing.getPhone()).isEqualTo("13800000000"); // 未被覆盖
    }

    @Test
    @DisplayName("toEntity — CustomerDTO 转 Customer 实体")
    void toEntity() {
        CustomerDTO dto = new CustomerDTO(
                10L, "映射客户", "13700137000", null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        Customer entity = customerMapper.toEntity(dto);
        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getName()).isEqualTo("映射客户");
        assertThat(entity.getPhone()).isEqualTo("13700137000");
    }
}
