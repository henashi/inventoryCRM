package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.GiftCreateDTO;
import com.henashi.inventorycrm.dto.GiftDTO;
import com.henashi.inventorycrm.dto.GiftUpdateDTO;
import com.henashi.inventorycrm.service.GiftService;
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
@RequestMapping("/api/gifts")
public class GiftController {

    private final GiftService giftService;

    @GetMapping("/{id}")
    public GiftDTO getGiftById(@PathVariable @NotNull @Min(value = 1, message = "ID必须大于0") Long id) {
        return giftService.findGiftById(id);
    }

    @GetMapping
    public Page<GiftDTO> getGifts(Pageable pageable) {
        return giftService.findGifts(pageable);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGift(@PathVariable @NotNull @Min(value = 1, message = "ID必须大于0") Long id) {
        giftService.deleteGiftById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<GiftDTO> createGift(@RequestBody @Valid GiftCreateDTO giftCreateDTO) {
        GiftDTO createdGift = giftService.saveGift(giftCreateDTO);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdGift.id())
                .toUri();
        return ResponseEntity.created(location).body(createdGift);
    }

    @PutMapping("/{id}")
    public GiftDTO updateGift(@PathVariable @NotNull @Min(value = 1, message = "ID必须大于0") Long id, @RequestBody @Valid GiftUpdateDTO giftUpdateDTO) {
        return giftService.updateGift(id, giftUpdateDTO);
    }
}
