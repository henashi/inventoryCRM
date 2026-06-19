package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ImportResultDTO(
        @Schema(description = "导入成功数量")
        Integer successCount,

        @Schema(description = "导入失败数量")
        Integer failureCount,

        @Schema(description = "失败明细")
        List<ImportFailureDetailDTO> failureDetails,

        @Schema(description = "导入模板字段")
        List<String> templateFields,

        @Schema(description = "必填字段")
        List<String> requiredFields,

        @Schema(description = "重复数据处理策略")
        String duplicateStrategy,

        @Schema(description = "默认值或额外说明")
        List<String> notes
) {
}
