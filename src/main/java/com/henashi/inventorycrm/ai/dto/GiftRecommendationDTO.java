package com.henashi.inventorycrm.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 礼品推荐 DTO
 */
@Schema(description = "礼品推荐结果")
public record GiftRecommendationDTO(

        @Schema(description = "礼品ID")
        Long giftId,

        @Schema(description = "礼品名称")
        String giftName,

        @Schema(description = "礼品编码")
        String giftCode,

        @Schema(description = "礼品类型")
        String giftType,

        @Schema(description = "匹配度分数 0-100")
        Double matchScore,

        @Schema(description = "推荐理由")
        String reason
) {}
