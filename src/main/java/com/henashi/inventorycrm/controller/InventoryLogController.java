package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.InventoryLogCreateDTO;
import com.henashi.inventorycrm.dto.InventoryLogDTO;
import com.henashi.inventorycrm.service.InventoryLogService;
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
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory-logs")
public class InventoryLogController {

    private final InventoryLogService inventoryLogService;

    @GetMapping("/{id}")
    public InventoryLogDTO getInventoryLog(
            @PathVariable @NotNull @Min(1) Long id) {
        return inventoryLogService.findInventoryLogDTOById(id);
    }

    @GetMapping
    public Page<InventoryLogDTO> getInventoryLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) LocalDate startTime,
            @RequestParam(required = false) LocalDate endTime
    ) {
        return inventoryLogService.findPage(page, size, productId, type, operator, startTime, endTime);
    }

    @GetMapping("/product/{productId}")
    public Page<InventoryLogDTO> getLogsByProductId(
            @PathVariable @NotNull Long productId,
            Pageable pageable) {
        return inventoryLogService.getLogsByProductId(productId, pageable);
    }

    @GetMapping("/type/{type}")
    public Page<InventoryLogDTO> getLogsByType(
            @PathVariable @NotNull String type,
            Pageable pageable) {
        return inventoryLogService.getLogsByType(type, pageable);
    }

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
}