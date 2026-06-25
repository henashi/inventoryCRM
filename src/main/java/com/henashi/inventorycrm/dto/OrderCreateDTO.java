package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "创建订单")
public record OrderCreateDTO(

    @NotNull(message = "客户ID不能为空")
    Long customerId,

    BigDecimal discount,

    String remark,

    @NotNull @Size(min = 1)
    @Valid
    List<OrderItemCreateDTO> items
) {}
