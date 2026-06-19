package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImportFailureDetailDTO(
        @Schema(description = "失败行号（包含表头时按文件实际行号返回）")
        Integer rowNumber,

        @Schema(description = "失败记录标识，如手机号或商品编码")
        String identifier,

        @Schema(description = "失败原因")
        String reason
) {
}
