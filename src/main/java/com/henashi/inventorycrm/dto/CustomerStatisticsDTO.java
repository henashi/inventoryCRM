package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record CustomerStatisticsDTO(
        @Schema(description = "客户总数")
        Long totalCustomers,

        @Schema(description = "正常客户数")
        Long normalCustomers,

        @Schema(description = "停用客户数")
        Long disabledCustomers,

        @Schema(description = "礼品等级分布，key 为 giftLevel")
        Map<Integer, Long> giftLevelDistribution
) {
}
