package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.InventoryLogCreateDTO;
import com.henashi.inventorycrm.dto.InventoryLogDTO;
import com.henashi.inventorycrm.dto.InventoryLogStatsDTO;
import com.henashi.inventorycrm.service.InventoryLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory-logs")
@Tag(name = "库存日志管理控制器", description = "提供库存日志相关的CRUD操作接口")
public class InventoryLogController {

    private final InventoryLogService inventoryLogService;

    @Operation(summary = "导出库存日志", description = "根据查询条件导出筛选后的库存日志")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportInventoryLogs(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endTime
    ) {
        byte[] content = inventoryLogService.exportInventoryLogs(productId, type, operator, startTime, endTime);
        String fileName = "inventory_logs_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        return buildCsvResponse(fileName, content);
    }

    @Operation(summary = "根据ID查询库存日志", description = "通过库存日志ID查询库存日志的详细信息")
    @GetMapping("/{id}")
    public InventoryLogDTO getInventoryLog(
            @PathVariable @NotNull @Min(1) Long id) {
        return inventoryLogService.findInventoryLogDTOById(id);
    }

    @Operation(summary = "分页查询库存日志", description = "根据查询条件分页查询库存日志信息")
    @GetMapping
    public Page<InventoryLogDTO> getInventoryLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endTime
    ) {
        return inventoryLogService.findPage(page, size, productId, type, operator, startTime, endTime);
    }

    @Operation(summary = "根据商品ID查询库存日志", description = "通过商品ID分页查询该商品的所有库存日志信息")
    @GetMapping("/product/{productId}")
    public Page<InventoryLogDTO> getLogsByProductId(
            @PathVariable @NotNull Long productId,
            Pageable pageable) {
        return inventoryLogService.getLogsByProductId(productId, pageable);
    }

    @Operation(summary = "根据操作类型查询库存日志", description = "通过操作类型分页查询所有该类型的库存日志信息")
    @GetMapping("/type/{type}")
    public Page<InventoryLogDTO> getLogsByType(
            @PathVariable @NotNull String type,
            Pageable pageable) {
        return inventoryLogService.getLogsByType(type, pageable);
    }

    @Operation(summary = "创建库存日志", description = "根据提供的库存日志信息创建一个新的库存日志")
    @PostMapping
    public ResponseEntity<InventoryLogDTO> createInventoryLog(
            @Valid @RequestBody InventoryLogCreateDTO logCreateDTO) {
        InventoryLogDTO savedLog = inventoryLogService.saveInventoryLog(logCreateDTO);
        URI location = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(savedLog.id())
                        .toUri();
         return ResponseEntity.created(location).body(savedLog);
    }

    @Operation(summary = "统计库存日志数据", description = "统计不同类型的库存日志数量和总变动数量")
    @GetMapping("/stats")
    public InventoryLogStatsDTO countStats() {
        return inventoryLogService.countStats();
    }

    private ResponseEntity<byte[]> buildCsvResponse(String fileName, byte[] content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(content.length)
                .body(content);
    }
}
