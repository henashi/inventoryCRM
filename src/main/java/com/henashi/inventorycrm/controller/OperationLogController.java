package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.OperationLogCreateDTO;
import com.henashi.inventorycrm.dto.OperationLogDTO;
import com.henashi.inventorycrm.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/operation-logs")
@Tag(name = "操作日志控制器", description = "提供操作日志的增删改查接口")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping("/{id}")
    @Operation(summary = "获取操作日志", description = "根据ID获取操作日志的详细信息")
    public OperationLogDTO getOperationLog(
            @PathVariable @NotNull @Min(1) Long id) {
        return operationLogService.findOperationLogDTOById(id);
    }

    @GetMapping("/module/{module}")
    @Operation(summary = "获取模块日志", description = "根据模块名称获取相关的操作日志")
    public Page<OperationLogDTO> getLogsByModule(
            @PathVariable @NotNull String module,
            @RequestParam(defaultValue = "5") Integer size,
            @RequestParam(defaultValue = "0") Integer page) {
        Sort sort = Sort.by("id").descending();
        return operationLogService.getLogsByModule(module, PageRequest.of(size, page, sort));
    }

    @GetMapping("/operator/{operator}")
    @Operation(summary = "获取操作人日志", description = "根据操作人名称获取相关的操作日志")
    public Page<OperationLogDTO> getLogsByOperator(
            @PathVariable @NotNull String operator,
            @RequestParam(defaultValue = "5") Integer size,
            @RequestParam(defaultValue = "0") Integer page) {
        Sort sort = Sort.by("id").descending();
        return operationLogService.getLogsByOperator(operator, PageRequest.of(size, page, sort));
    }

    @GetMapping("/search")
    @Operation(summary = "分页查询日志", description = "根据关键词搜索操作日志，支持模块名称、操作人和描述的模糊匹配")
    public Page<OperationLogDTO> searchLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "5") Integer size,
            @RequestParam(defaultValue = "0") Integer page
    ) {
        Sort sort = Sort.by("id").descending();
        return operationLogService.searchLogs(keyword, PageRequest.of(size, page, sort));
    }

    @PostMapping
    @Operation(summary = "创建操作日志", description = "记录一条新的操作日志")
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