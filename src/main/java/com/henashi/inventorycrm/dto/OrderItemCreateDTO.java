package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "创建消费记录")
public record OrderItemCreateDTO(
    Long customerId,

    Long productId,

    String productName,

    @NotNull @Min(1)
    Integer quantity,

    BigDecimal unitPrice,

    @NotNull
    BigDecimal totalAmount,

    String remark
) {}
