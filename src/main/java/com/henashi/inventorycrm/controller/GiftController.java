package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.GiftCreateDTO;
import com.henashi.inventorycrm.dto.GiftDTO;
import com.henashi.inventorycrm.dto.GiftUpdateDTO;
import com.henashi.inventorycrm.service.GiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gifts")
@Tag(name = "礼品管理控制器", description = "提供礼品相关的CRUD操作接口")
public class GiftController {

    private final GiftService giftService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取礼品信息", description = "通过礼品ID查询礼品的详细信息")
    public GiftDTO getGiftById(@PathVariable @NotNull @Min(value = 1, message = "ID必须大于0") Long id) {
        return giftService.findGiftById(id);
    }

    @GetMapping
    @Operation(summary = "获取礼品列表", description = "分页查询所有礼品信息")
    public Page<GiftDTO> getGifts(
            @RequestParam(defaultValue = "5") Integer size,
            @RequestParam(defaultValue = "0") Integer page
    ) {
        Sort sort = Sort.by("contentUpdatedTime", "createdTime").descending();
        return giftService.findGifts(PageRequest.of(page, size, sort));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "根据ID删除礼品", description = "通过礼品ID删除对应的礼品信息")
    public ResponseEntity<Void> deleteGift(@PathVariable @NotNull @Min(value = 1, message = "ID必须大于0") Long id) {
        giftService.deleteGiftById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @Operation(summary = "创建新礼品", description = "根据提供的礼品信息创建一个新的礼品")
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
    @Operation(summary = "更新礼品信息", description = "根据提供的礼品ID和更新信息修改对应的礼品")
    public GiftDTO updateGift(@PathVariable @NotNull @Min(value = 1, message = "ID必须大于0") Long id, @RequestBody @Valid GiftUpdateDTO giftUpdateDTO) {
        return giftService.updateGift(id, giftUpdateDTO);
    }
}
