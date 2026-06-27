package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.OperationLogCreateDTO;
import com.henashi.inventorycrm.dto.OperationLogDTO;
import com.henashi.inventorycrm.annotation.OperationLogIgnore;
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
            @PathVariable("id") @NotNull @Min(1) Long id) {
        return operationLogService.findOperationLogDTOById(id);
    }

    @GetMapping("/module/{module}")
    @Operation(summary = "获取模块日志", description = "根据模块名称获取相关的操作日志")
    public Page<OperationLogDTO> getLogsByModule(
            @PathVariable("module") @NotNull String module,
            @RequestParam(name = "size", defaultValue = "5") Integer size,
            @RequestParam(name = "page", defaultValue = "0") Integer page) {
        Sort sort = Sort.by("id").descending();
        return operationLogService.getLogsByModule(module, PageRequest.of(page, size, sort));
    }

    @GetMapping("/operator/{operator}")
    @Operation(summary = "获取操作人日志", description = "根据操作人名称获取相关的操作日志")
    public Page<OperationLogDTO> getLogsByOperator(
            @PathVariable("operator") @NotNull String operator,
            @RequestParam(name = "size", defaultValue = "5") Integer size,
            @RequestParam(name = "page", defaultValue = "0") Integer page) {
        Sort sort = Sort.by("id").descending();
        return operationLogService.getLogsByOperator(operator, PageRequest.of(page, size, sort));
    }

    @GetMapping("/search")
    @Operation(summary = "分页查询日志", description = "多条件搜索操作日志，支持关键词/模块/操作人/时间范围过滤")
    public Page<OperationLogDTO> searchLogs(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "module", required = false) String module,
            @RequestParam(name = "operator", required = false) String operator,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "startTime", required = false) String startTime,
            @RequestParam(name = "endTime", required = false) String endTime,
            @RequestParam(name = "size", defaultValue = "20") Integer size,
            @RequestParam(name = "page", defaultValue = "0") Integer page
    ) {
        Sort sort = Sort.by("id").descending();
        return operationLogService.searchLogs(keyword, module, operator, status, startTime, endTime, PageRequest.of(page, size, sort));
    }

    @PostMapping
    @OperationLogIgnore
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