package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.GiftLogCreateDTO;
import com.henashi.inventorycrm.dto.GiftLogDTO;
import com.henashi.inventorycrm.dto.GiftLogUpdateDTO;
import com.henashi.inventorycrm.service.GiftLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gift-logs")
@Tag(name = "礼品日志管理控制器", description = "提供礼品日志相关的CRUD操作接口")
public class GiftLogController {

    private final GiftLogService giftLogService;

    @GetMapping
    @Operation(summary = "获取礼品日志列表", description = "分页查询所有礼品日志信息")
    public Page<GiftLogDTO> loadGiftLogPage(Pageable pageable) {
        return giftLogService.loadGiftLogPage(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取礼品日志信息", description = "通过礼品日志ID查询礼品日志的详细信息")
    public GiftLogDTO getGiftLog(
            @PathVariable @NotNull @Min(1) Long id) {
        return giftLogService.findGiftLogDTOById(id);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "根据客户ID获取礼品日志列表", description = "通过客户ID分页查询该客户的所有礼品日志信息")
    public Page<GiftLogDTO> getLogsByCustomerId(
            @PathVariable @NotNull Long customerId,
            Pageable pageable) {
        return giftLogService.getLogsByCustomerId(customerId, pageable);
    }

//    @GetMapping("/level/{level}")
//    public ResponseEntity<Page<GiftLogDTO>> getLogsByGiftLevel(
//            @PathVariable @NotNull Integer level,
//            Pageable pageable) {
//        return ResponseEntity.ok(giftLogService.getLogsByGiftLevel(level, pageable));
//    }

    @GetMapping("/customer/{customerId}/gift-level")
    @Operation(summary = "获取客户的礼品等级", description = "根据客户ID查询该客户当前的礼品等级")
    public Integer getCustomerGiftLevel(
            @PathVariable @NotNull Long customerId) {
        log.info("请求查询客户 {} 的礼品等级", customerId);
        return giftLogService.getCustomerGiftLevel(customerId);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "更新礼品日志信息", description = "根据提供的礼品日志ID和更新信息修改对应的礼品日志")
    public GiftLogDTO updateGiftLog(@PathVariable @NotNull Long id, @Valid @RequestBody GiftLogUpdateDTO giftLogUpdateDTO) {
        return giftLogService.updateGiftLog(id, giftLogUpdateDTO);
    }

    @PostMapping
    @Operation(summary = "创建新礼品日志", description = "根据提供的礼品日志信息创建一个新的礼品日志")
    public ResponseEntity<GiftLogDTO> createGiftLog(
            @Valid @RequestBody GiftLogCreateDTO logCreateDTO) {
        GiftLogDTO savedLog = giftLogService.saveGiftLog(logCreateDTO);
        URI location = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(savedLog.id())
                        .toUri();
         return ResponseEntity.created(location).body(savedLog);
    }
}