package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.SystemConfigCreateDTO;
import com.henashi.inventorycrm.dto.SystemConfigDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.SystemConfigMapper;
import com.henashi.inventorycrm.pojo.SystemConfig;
import com.henashi.inventorycrm.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final Logger log = LoggerFactory.getLogger(SystemConfigService.class);
    private final SystemConfigRepository systemConfigRepository;
    private final SystemConfigMapper systemConfigMapper;

    public SystemConfigDTO findSystemConfigDTOById(Long configId) {
        log.debug("查询系统配置详情: id={}", configId);
        if (configId == null || configId <= 0) {
            log.warn("配置ID无效: id={}", configId);
            throw new IllegalArgumentException("配置ID无效");
        }
        return systemConfigRepository.findById(configId)
                .map(config -> {
                    log.info("找到系统配置: {} = {}", config.getConfigKey(), config.getConfigValue());
                    return systemConfigMapper.fromEntity(config);
                })
                .orElseThrow(() -> {
                    log.warn("系统配置不存在: id={}", configId);
                    return new BusinessException("CONFIG_NOT_FOUND",
                            String.format("系统配置(ID: %d)不存在", configId));
                });
    }

    @Cacheable(value = "systemConfig", key = "#configKey")
    public String getConfigValue(String configKey) {
        return systemConfigRepository.findByConfigKey(configKey)
                .map(SystemConfig::getConfigValue)
                .orElseThrow(() -> new BusinessException("CONFIG_NOT_FOUND",
                        String.format("配置项 %s 不存在", configKey)));
    }

    @Cacheable(value = "systemConfigs", key = "#configGroup")
    public List<SystemConfigDTO> getConfigsByGroup(String configGroup) {
        return systemConfigRepository.findByConfigGroup(configGroup)
                .stream()
                .map(systemConfigMapper::fromEntity)
                .collect(Collectors.toList());
    }

    public Map<String, String> getConfigsAsMap(String configGroup) {
        return systemConfigRepository.findByConfigGroup(configGroup)
                .stream()
                .collect(Collectors.toMap(
                        SystemConfig::getConfigKey,
                        SystemConfig::getConfigValue
                ));
    }

    @Transactional
    @CacheEvict(value = {"systemConfig", "systemConfigs"}, allEntries = true)
    public SystemConfigDTO saveSystemConfig(SystemConfigCreateDTO configCreateDTO) {
        log.debug("创建系统配置: key={}", configCreateDTO.configKey());

        // 验证配置键是否重复
        if (systemConfigRepository.existsByConfigKey(configCreateDTO.configKey())) {
            throw new BusinessException("CONFIG_KEY_EXISTS",
                    String.format("配置键 %s 已存在", configCreateDTO.configKey()));
        }

        SystemConfig config = systemConfigMapper.createToEntity(configCreateDTO);
        SystemConfig saved = systemConfigRepository.save(config);

        log.info("系统配置创建成功: {} = {}", saved.getConfigKey(), saved.getConfigValue());
        return systemConfigMapper.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(value = {"systemConfig", "systemConfigs"}, allEntries = true)
    public SystemConfigDTO updateSystemConfig(Long id, SystemConfigCreateDTO dto) {
        if (id == null || id <= 0L) {
            log.warn("更新系统配置信息异常: {}", dto);
            throw new IllegalArgumentException("配置ID无效");
        }

        SystemConfig existingConfig = systemConfigRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("未找到系统配置: {}", id);
                    return new BusinessException("CONFIG_NOT_FOUND",
                            String.format("系统配置(ID: %d)不存在", id));
                });

        log.info("找到系统配置信息: {} = {}", existingConfig.getConfigKey(), existingConfig.getConfigValue());

        // 验证配置键是否重复（如果修改了配置键）
        if (dto.configKey() != null && !existingConfig.getConfigKey().equals(dto.configKey())) {
            if (systemConfigRepository.existsByConfigKey(dto.configKey())) {
                throw new BusinessException("CONFIG_KEY_EXISTS",
                        String.format("配置键 %s 已存在", dto.configKey()));
            }
        }

        // 更新配置
        updateConfigFields(existingConfig, dto);

        SystemConfig saved = systemConfigRepository.save(existingConfig);
        log.info("系统配置更新成功: {} = {}", saved.getConfigKey(), saved.getConfigValue());

        return systemConfigMapper.fromEntity(saved);
    }

    @Transactional
    @CacheEvict(value = {"systemConfig", "systemConfigs"}, allEntries = true)
    public SystemConfigDTO updateConfigValue(String configKey, String configValue) {
        SystemConfig config = systemConfigRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new BusinessException("CONFIG_NOT_FOUND",
                        String.format("配置项 %s 不存在", configKey)));

        config.setConfigValue(configValue);
        SystemConfig savedConfig = systemConfigRepository.save(config);
        log.info("系统配置值更新成功: {} = {}", configKey, configValue);
        return systemConfigMapper.fromEntity(savedConfig);
    }

    @Transactional
    @CacheEvict(value = {"systemConfig", "systemConfigs"}, allEntries = true)
    public void deleteById(Long id) {
        if (id == null || id <= 0L) {
            log.warn("系统配置ID异常: {}", id);
            throw new IllegalArgumentException("配置ID无效");
        }

        SystemConfig config = systemConfigRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("根据ID未找到系统配置: {}", id);
                    return new BusinessException("CONFIG_NOT_FOUND",
                            String.format("系统配置(ID: %d)不存在", id));
                });

        systemConfigRepository.delete(config);
        log.info("系统配置删除成功: {} = {}", config.getConfigKey(), config.getConfigValue());
    }

    private void updateConfigFields(SystemConfig config, SystemConfigCreateDTO dto) {
        if (dto.configKey() != null && !config.getConfigKey().equals(dto.configKey())) {
            config.setConfigKey(dto.configKey());
        }
        if (dto.configValue() != null) {
            config.setConfigValue(dto.configValue());
        }
        if (dto.description() != null) {
            config.setDescription(dto.description());
        }
        if (dto.configGroup() != null) {
            config.setConfigGroup(dto.configGroup());
        }
    }
}