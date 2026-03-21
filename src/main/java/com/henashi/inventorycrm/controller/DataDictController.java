package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.DataDictCreateDTO;
import com.henashi.inventorycrm.dto.DataDictDTO;
import com.henashi.inventorycrm.dto.DataDictUpdateDTO;
import com.henashi.inventorycrm.service.DataDictService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/data-dict")
@RequiredArgsConstructor
public class DataDictController {

    public final DataDictService dataDictService;

    @GetMapping
    public Page<DataDictDTO> findDataDictPage(
            @RequestParam(defaultValue = "5") Integer size,
            @RequestParam(defaultValue = "0") Integer page
    ) {
        Sort sort = Sort.by("updatedTime", "createdTime").descending();
        return dataDictService.findDataDictPage(PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public DataDictDTO findById(
            @PathVariable
            @NotNull
            @Min(value = 1, message = "ID必须大于0")
            Long id) {
        return dataDictService.findById(id);
    }

    @PostMapping
    public ResponseEntity<DataDictDTO> createDataDict(@RequestBody @Valid DataDictCreateDTO dataDictCreateDTO) {
        DataDictDTO dataDict = dataDictService.createDataDict(dataDictCreateDTO);
        URI uri = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(dataDict.id())
                    .toUri();
        return ResponseEntity.created(uri).body(dataDict);
    }

    @PutMapping("/{id}")
    public DataDictDTO updateDataDict(
            @PathVariable
            @NotNull
            @Min(value = 1, message = "ID必须大于0")
            Long id,
            @RequestBody @Valid DataDictUpdateDTO dataDictUpdateDTO) {
        return dataDictService.updateDataDict(id, dataDictUpdateDTO);
    }

    @DeleteMapping("/{id}")
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
