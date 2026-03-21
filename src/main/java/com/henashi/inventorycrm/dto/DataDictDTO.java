package com.henashi.inventorycrm.dto;

import com.henashi.inventorycrm.pojo.DataDict;

public record DataDictDTO (
        Long id,
        String paramName,
        String paramCode,
        String paramValue,
        String groupName,
        String groupCode,
        String description,
        DataDict.DataDictStatus status
) {
}
