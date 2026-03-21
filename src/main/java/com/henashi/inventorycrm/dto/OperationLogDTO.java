package com.henashi.inventorycrm.dto;

import java.time.LocalDateTime;

public record OperationLogDTO(
        Long id,
        String module,
        String operationType,
        String description,
        String requestUrl,
        String requestMethod,
        String operator,
        String ipAddress,
        Integer status,
        String errorMessage,
        Long executionTime,
        LocalDateTime operationTime
) {
}

