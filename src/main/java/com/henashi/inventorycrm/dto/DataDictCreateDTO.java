package com.henashi.inventorycrm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record DataDictCreateDTO(
        @Schema(description = "参数编码，必填")
        @NotNull
        @Length(max = 50)
        String paramCode,

        @Schema(description = "参数名称，必填")
        @NotNull
        @Length(max = 50)
        String paramName,

        @Schema(description = "参数值")
        @Length(max = 200)
        String paramValue,

        @Schema(description = "分组编码，必填")
        @NotNull
        @Length(max = 50)
        String groupCode,

        @Schema(description = "分组名称，必填")
        @NotNull
        @Length(max = 50)
        String groupName,

        @Schema(description = "描述")
        @Length(max = 200)
        String description
) {
}
