package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.DataDict;
import org.hibernate.validator.constraints.Length;

public record DataDictUpdateDTO(
        @Length(max = 50)
        String paramCode,
        @Length(max = 200)
        String paramValue,
        @Length(max = 50)
        String paramName,
        @Length(max = 50)
        String groupCode,
        @Length(max = 50)
        String groupName,
        @Length(max = 200)
        String description,
        DataDict.DataDictStatus status
) {
}
