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

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system-configs")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/{id}")
    public SystemConfigDTO getSystemConfig(
            @PathVariable @NotNull @Min(1) Long id) {
        return systemConfigService.findSystemConfigDTOById(id);
    }

    @GetMapping("/key/{key}")
    public String getConfigValue(
            @PathVariable @NotNull String key) {
        return systemConfigService.getConfigValue(key);
    }

    @GetMapping("/group/{group}")
    public List<SystemConfigDTO> getConfigsByGroup(
            @PathVariable @NotNull String group) {
        return systemConfigService.getConfigsByGroup(group);
    }

    @GetMapping("/group/{group}/map")
    public Map<String, String> getConfigsAsMap(
            @PathVariable @NotNull String group) {
        return systemConfigService.getConfigsAsMap(group);
    }

    @PostMapping
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
    public SystemConfigDTO updateSystemConfig(
            @PathVariable @NotNull @Min(1) Long id,
            @Valid @RequestBody SystemConfigCreateDTO configCreateDTO) {
        return systemConfigService.updateSystemConfig(id, configCreateDTO);
    }

    @PutMapping("/key/{key}/value")
    public SystemConfigDTO updateConfigValue(
            @PathVariable @NotNull String key,
            @RequestBody Map<String, String> request) {
        String value = request.get("value");
        return systemConfigService.updateConfigValue(key, value);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSystemConfig(
            @PathVariable @NotNull @Min(1) Long id) {
        systemConfigService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}