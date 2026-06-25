package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "消费记录")
public record OrderItemDTO(
    Long id,
    Long customerId,
    String customerName,
    Long productId,
    String productName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalAmount,
    LocalDateTime orderTime,
    String remark,
    LocalDateTime createdTime
) {}
