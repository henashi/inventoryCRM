package com.henashi.inventorycrm.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 单个商品的库存预测结果
 */
@Schema(description = "库存预测结果")
public record StockPredictionDTO(

        @Schema(description = "商品ID")
        Long productId,

        @Schema(description = "商品编码")
        String productCode,

        @Schema(description = "商品名称")
        String productName,

        @Schema(description = "商品分类")
        String category,

        @Schema(description = "单位")
        String unit,

        @Schema(description = "当前库存")
        Integer currentStock,

        @Schema(description = "安全库存")
        Integer safeStock,

        @Schema(description = "近7日均出库量")
        BigDecimal avgDailyOut7d,

        @Schema(description = "近30日均出库量")
        BigDecimal avgDailyOut30d,

        @Schema(description = "预计耗尽天数（-1表示不出库永不耗尽）")
        Integer estimatedDaysToEmpty,

        @Schema(description = "预计安全天数（低于安全库存的天数，-1表示不出库）")
        Integer estimatedDaysToSafe,

        @Schema(description = "建议补货量")
        Integer suggestedRestockQty,

        @Schema(description = "预警级别: NORMAL / WARNING / DANGER")
        String alertLevel,

        @Schema(description = "预警原因")
        String alertReason,

        @Schema(description = "OLS 回归斜率（每日出库变化量，>0 表示出库加速）")
        Double slope,

        @Schema(description = "OLS 回归 R²（拟合优度 0~1，越接近1越可信）")
        Double rSquared,

        @Schema(description = "趋势方向: UP / DOWN / STABLE")
        String trendDirection,

        @Schema(description = "历史出库明细（近30天，用于前端画趋势图）")
        List<DailyOutRecord> dailyOutRecords

) {

    @Schema(description = "每日出库记录")
    public record DailyOutRecord(
            @Schema(description = "日期")
            LocalDate date,

            @Schema(description = "出库量")
            Integer quantity
    ) {}
}
