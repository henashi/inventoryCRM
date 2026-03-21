package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.OperationLogCreateDTO;
import com.henashi.inventorycrm.dto.OperationLogDTO;
import com.henashi.inventorycrm.service.OperationLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/operation-logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/{id}")
    public OperationLogDTO getOperationLog(
            @PathVariable @NotNull @Min(1) Long id) {
        return operationLogService.findOperationLogDTOById(id);
    }

    @GetMapping("/module/{module}")
    public Page<OperationLogDTO> getLogsByModule(
            @PathVariable @NotNull String module,
            Pageable pageable) {
        return operationLogService.getLogsByModule(module, pageable);
    }

    @GetMapping("/operator/{operator}")
    public Page<OperationLogDTO> getLogsByOperator(
            @PathVariable @NotNull String operator,
            Pageable pageable) {
        return operationLogService.getLogsByOperator(operator, pageable);
    }

    @GetMapping("/search")
    public Page<OperationLogDTO> searchLogs(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        return operationLogService.searchLogs(keyword, pageable);
    }

    @PostMapping
    public ResponseEntity<OperationLogDTO> createOperationLog(
            @Valid @RequestBody OperationLogCreateDTO logCreateDTO) {
        OperationLogDTO savedLog = operationLogService.saveOperationLog(logCreateDTO);
        URI location = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(savedLog.id())
                        .toUri();
         return ResponseEntity.created(location).body(savedLog);
    }
}