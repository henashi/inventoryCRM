package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.DataDictCreateDTO;
import com.henashi.inventorycrm.dto.DataDictDTO;
import com.henashi.inventorycrm.dto.DataDictUpdateDTO;
import com.henashi.inventorycrm.service.DataDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/data-dict")
@RequiredArgsConstructor
@Tag(name = "数据字典管理", description = "提供数据字典的增删改查接口")
public class DataDictController {

    public final DataDictService dataDictService;

    @GetMapping
    @Operation(summary = "分页查询数据字典", description = "根据分页参数查询数据字典列表，默认每页5条，第一页")
    public Page<DataDictDTO> findDataDictPage(
            @RequestParam(defaultValue = "5") Integer size,
            @RequestParam(defaultValue = "0") Integer page
    ) {
        Sort sort = Sort.by("contentUpdatedTime", "createdTime").descending();
        return dataDictService.findDataDictPage(PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询数据字典", description = "根据数据字典ID查询对应的数据字典信息")
    public DataDictDTO findById(
            @PathVariable
            @NotNull
            @Min(value = 1, message = "ID必须大于0")
            Long id) {
        return dataDictService.findById(id);
    }

    @PostMapping
    @Operation(summary = "创建数据字典", description = "根据提供的数据字典信息创建新的数据字典")
    public ResponseEntity<DataDictDTO> createDataDict(@RequestBody @Valid DataDictCreateDTO dataDictCreateDTO) {
        DataDictDTO dataDict = dataDictService.createDataDict(dataDictCreateDTO);
        URI uri = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(dataDict.id())
                    .toUri();
        return ResponseEntity.created(uri).body(dataDict);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "更新数据字典", description = "根据数据字典ID和提供的更新信息修改对应的数据字典")
    public DataDictDTO updateDataDict(
            @PathVariable
            @NotNull
            @Min(value = 1, message = "ID必须大于0")
            Long id,
            @RequestBody @Valid DataDictUpdateDTO dataDictUpdateDTO) {
        return dataDictService.updateDataDict(id, dataDictUpdateDTO);
    }

    @PatchMapping("/status/{id}/{enable}")
    @Operation(summary = "更新数据字典", description = "根据数据字典ID和提供的更新信息修改对应的数据字典")
    public DataDictDTO updateStatus(
            @PathVariable
            @NotNull
            @Min(value = 1, message = "ID必须大于0")
            Long id,
            @PathVariable
            @NotNull
            Boolean enable) {
        return dataDictService.enableOrDisable(id, enable);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除数据字典", description = "根据数据字典ID删除对应的数据字典")
    public ResponseEntity<Void> deleteById(
            @NotNull
            @PathVariable
            @Min(value = 1, message = "ID必须大于0")
            Long id
    ) {
        dataDictService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
