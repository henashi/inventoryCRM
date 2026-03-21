package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.GiftLogCreateDTO;
import com.henashi.inventorycrm.dto.GiftLogDTO;
import com.henashi.inventorycrm.dto.GiftLogUpdateDTO;
import com.henashi.inventorycrm.service.GiftLogService;
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
@RequestMapping("/api/gift-logs")
public class GiftLogController {

    private final GiftLogService giftLogService;

    @GetMapping
    public Page<GiftLogDTO> loadGiftLogPage(Pageable pageable) {
        return giftLogService.loadGiftLogPage(pageable);
    }

    @GetMapping("/{id}")
    public GiftLogDTO getGiftLog(
            @PathVariable @NotNull @Min(1) Long id) {
        return giftLogService.findGiftLogDTOById(id);
    }

    @GetMapping("/customer/{customerId}")
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
    public Integer getCustomerGiftLevel(
            @PathVariable @NotNull Long customerId) {
        return giftLogService.getCustomerGiftLevel(customerId);
    }

    @PatchMapping("/{id}")
    public GiftLogDTO updateGiftLog(@PathVariable @NotNull Long id, @Valid @RequestBody GiftLogUpdateDTO giftLogUpdateDTO) {
        return giftLogService.updateGiftLog(id, giftLogUpdateDTO);
    }

    @PostMapping
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