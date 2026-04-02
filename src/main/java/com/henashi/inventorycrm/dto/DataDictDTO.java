package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.henashi.inventorycrm.pojo.DataDict;

public record DataDictDTO (
        @Schema(description = "主键ID")
        Long id,

        @Schema(description = "参数名称")
        String paramName,

        @Schema(description = "参数编码")
        String paramCode,

        @Schema(description = "参数值")
        String paramValue,

        @Schema(description = "分组名称")
        String groupName,

        @Schema(description = "分组编码")
        String groupCode,

        @Schema(description = "描述")
        String description,

        @Schema(description = "状态，枚举值")
        DataDict.DataDictStatus status
) {
}
