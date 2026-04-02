package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.DataDict;
import io.swagger.v3.oas.annotations.media.Schema;
import org.hibernate.validator.constraints.Length;

public record DataDictUpdateDTO(
        @Length(max = 50)
        @Schema(description = "参数编码")
        String paramCode,
        @Schema(description = "参数值")
        @Length(max = 200)
        String paramValue,
        @Schema(description = "参数名称")
        @Length(max = 50)
        String paramName,
        @Schema(description = "分组编码")
        @Length(max = 50)
        String groupCode,
        @Schema(description = "分组名称")
        @Length(max = 50)
        String groupName,
        @Schema(description = "描述")
        @Length(max = 200)
        String description,
        @Schema(description = "状态，枚举值")
        DataDict.DataDictStatus status
) {
}
