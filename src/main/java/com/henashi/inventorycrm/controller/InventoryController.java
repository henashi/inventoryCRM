package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.annotation.RequirePermission;
import static com.henashi.inventorycrm.constants.Permissions.*;
import com.henashi.inventorycrm.dto.InventoryAdjustDTO;
import com.henashi.inventorycrm.dto.InventoryChangeDTO;
import com.henashi.inventorycrm.dto.InventoryDTO;
import com.henashi.inventorycrm.dto.InventoryDetailDTO;
import com.henashi.inventorycrm.dto.InventoryInDTO;
import com.henashi.inventorycrm.dto.InventoryOutDTO;
import com.henashi.inventorycrm.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventories")
@Tag(name = "库存视图控制器", description = "提供统一库存视图、库存变更和库存导出能力")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "分页查询库存快照", description = "基于商品主数据返回库存分页视图")
    public Page<InventoryDTO> getInventories(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "lowStockOnly", required = false) Boolean lowStockOnly,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "minStock", required = false) Integer minStock,
            @RequestParam(name = "maxStock", required = false) Integer maxStock
    ) {
        return inventoryService.findInventories(page, size, keyword, lowStockOnly, status, productId, minStock, maxStock);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询库存详情", description = "按商品ID查询库存详情及最近库存变更记录")
    public InventoryDetailDTO getInventory(
            @PathVariable("id") @NotNull @Min(1) Long id) {
        return inventoryService.findInventoryDetail(id);
    }

    @PostMapping("/in")
    @RequirePermission(INVENTORY_STOCK_IN)
    @Operation(summary = "库存入库", description = "按统一库存规则执行标准入库并写入库存日志")
    public InventoryDTO stockIn(@Valid @RequestBody InventoryInDTO request) {
        return inventoryService.stockIn(request);
    }

    @PostMapping("/out")
    @RequirePermission(INVENTORY_STOCK_OUT)
    @Operation(summary = "库存出库", description = "按统一库存规则执行标准出库并写入库存日志")
    public InventoryDTO stockOut(@Valid @RequestBody InventoryOutDTO request) {
        return inventoryService.stockOut(request);
    }

    @PatchMapping("/{id}/adjust")
    @RequirePermission(INVENTORY_ADJUST)
    @Operation(summary = "库存调整", description = "按商品ID执行库存盘点修正并写入调整日志")
    public InventoryDTO adjustStock(
            @PathVariable("id") @NotNull @Min(1) Long id,
            @Valid @RequestBody InventoryAdjustDTO request) {
        return inventoryService.adjustStock(id, request);
    }

    @GetMapping("/history")
    @Operation(summary = "分页查询库存变更历史", description = "复用库存日志口径返回库存变更历史")
    public Page<InventoryChangeDTO> getInventoryHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "operator", required = false) String operator,
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endTime
    ) {
        return inventoryService.findInventoryHistory(page, size, productId, type, operator, startTime, endTime);
    }

    @GetMapping("/alerts")
    @Operation(summary = "查询库存预警", description = "返回低库存预警列表，支持统一阈值覆盖")
    public List<InventoryDTO> getAlerts(@RequestParam(name = "threshold", required = false) Integer threshold) {
        return inventoryService.findAlerts(threshold);
    }

    @GetMapping("/export")
    @Operation(summary = "导出库存快照", description = "按库存视图筛选条件导出当前库存快照")
    public ResponseEntity<byte[]> exportInventories(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "lowStockOnly", required = false) Boolean lowStockOnly,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "productId", required = false) Long productId,
            @RequestParam(name = "minStock", required = false) Integer minStock,
            @RequestParam(name = "maxStock", required = false) Integer maxStock
    ) {
        byte[] content = inventoryService.exportInventories(keyword, lowStockOnly, status, productId, minStock, maxStock);
        String fileName = "inventories_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        return buildCsvResponse(fileName, content);
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
