package com.henashi.inventorycrm.controller;

import com.henashi.inventorycrm.dto.SystemConfigCreateDTO;
import com.henashi.inventorycrm.dto.SystemConfigDTO;
import com.henashi.inventorycrm.service.SystemConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system-configs")
@Tag(name = "系统配置", description = "系统配置相关的API")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取系统配置")
    public SystemConfigDTO getSystemConfig(
            @PathVariable @NotNull @Min(1) Long id) {
        return systemConfigService.findSystemConfigDTOById(id);
    }

    @GetMapping("/key/{key}")
    @Operation(summary = "根据Key获取配置值", description = "通过配置Key获取对应的配置值")
    public String getConfigValue(
            @PathVariable @NotNull String key) {
        return systemConfigService.getConfigValue(key);
    }

    @GetMapping("/group/{group}")
    @Operation(summary = "根据分组获取配置列表", description = "通过分组名称获取系统配置列表")
    public List<SystemConfigDTO> getConfigsByGroup(
            @PathVariable @NotNull String group) {
        return systemConfigService.getConfigsByGroup(group);
    }

    @GetMapping("/group/{group}/map")
    @Operation(summary = "根据分组获取配置键值对", description = "通过分组名称获取系统配置的键值对列表")
    public Map<String, String> getConfigsAsMap(
            @PathVariable @NotNull String group) {
        return systemConfigService.getConfigsAsMap(group);
    }

    @PostMapping
    @Operation(summary = "创建系统配置", description = "添加一个新的系统配置项")
    public ResponseEntity<SystemConfigDTO> createSystemConfig(
            @Valid @RequestBody SystemConfigCreateDTO configCreateDTO) {
        SystemConfigDTO savedConfig = systemConfigService.saveSystemConfig(configCreateDTO);
        URI location = ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(savedConfig.id())
                        .toUri();
         return ResponseEntity.created(location).body(savedConfig);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新系统配置", description = "根据ID更新系统配置项的详细信息")
    public SystemConfigDTO updateSystemConfig(
            @PathVariable @NotNull @Min(1) Long id,
            @Valid @RequestBody SystemConfigCreateDTO configCreateDTO) {
        return systemConfigService.updateSystemConfig(id, configCreateDTO);
    }

    @PutMapping("/key/{key}/value")
    @Operation(summary = "更新配置值", description = "根据配置Key更新配置项的值")
    public SystemConfigDTO updateConfigValue(
            @PathVariable @NotNull String key,
            @RequestBody Map<String, String> request) {
        String value = request.get("value");
        return systemConfigService.updateConfigValue(key, value);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除系统配置", description = "根据ID删除系统配置项")
    public ResponseEntity<Void> deleteSystemConfig(
            @PathVariable @NotNull @Min(1) Long id) {
        systemConfigService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}