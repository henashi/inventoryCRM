package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "订单")
public record OrderDTO(
    Long id,
    Long customerId,
    String customerName,
    BigDecimal totalAmount,
    BigDecimal discount,
    BigDecimal finalAmount,
    LocalDateTime orderTime,
    String remark,
    List<OrderItemDTO> items,
    LocalDateTime createdTime
) {}
