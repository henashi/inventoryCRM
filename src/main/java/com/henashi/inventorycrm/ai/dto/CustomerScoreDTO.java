package com.henashi.inventorycrm.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 客户评分结果 DTO（含六维雷达数据）
 */
@Schema(description = "客户评分结果")
public record CustomerScoreDTO(

        @Schema(description = "客户ID")
        Long customerId,

        @Schema(description = "客户姓名")
        String customerName,

        @Schema(description = "手机号")
        String phone,

        @Schema(description = "礼品等级 0-3")
        Integer giftLevel,

        @Schema(description = "总分 0-100")
        Double totalScore,

        @Schema(description = "分段: HIGH_VALUE / GROWING / INACTIVE")
        String segment,

        @Schema(description = "六维评分明细 {维度名: 分数}")
        Map<String, Double> dimensionScores,

        @Schema(description = "是否未来7天生日的客户")
        Boolean isBirthdaySoon,

        @Schema(description = "生日（如果有）")
        String birthday,

        @Schema(description = "距离生日天数")
        Integer daysToBirthday,

        @Schema(description = "客户状态")
        String status
) {}
