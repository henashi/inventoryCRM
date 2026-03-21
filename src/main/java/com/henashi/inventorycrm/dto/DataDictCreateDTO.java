package com.henashi.inventorycrm.dto;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record DataDictCreateDTO(
        @NotNull
        @Length(max = 50)
        String paramCode,
        @NotNull
        @Length(max = 50)
        String paramName,
        @Length(max = 200)
        String paramValue,
        @NotNull
        @Length(max = 50)
        String groupCode,
        @NotNull
        @Length(max = 50)
        String groupName,
        @Length(max = 200)
        String description
) {
}
