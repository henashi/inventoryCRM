package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.SystemConfigCreateDTO;
import com.henashi.inventorycrm.dto.SystemConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SystemConfigServiceTest {

    @Autowired private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test @DisplayName("创建配置")
    void create() {
        // SystemConfigCreateDTO: (configKey, configValue, description, configGroup)
        SystemConfigCreateDTO dto = new SystemConfigCreateDTO("TEST_KEY", "test_value", "测试配置", "TEST_GROUP");
        SystemConfigDTO r = systemConfigService.saveSystemConfig(dto);
        assertThat(r.id()).isNotNull();
        assertThat(r.configValue()).isEqualTo("test_value");
    }

    @Test @DisplayName("创建配置 — 重复 key 抛异常")
    void createDuplicateKey() {
        SystemConfigCreateDTO dto = new SystemConfigCreateDTO("DUP_KEY", "val", "测试", "GRP");
        systemConfigService.saveSystemConfig(dto);
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> systemConfigService.saveSystemConfig(dto));
    }

    @Test @DisplayName("查询 — 根据 ID")
    void findById() {
        SystemConfigCreateDTO dto = new SystemConfigCreateDTO("FIND_KEY", "val", "测试", "GRP");
        SystemConfigDTO created = systemConfigService.saveSystemConfig(dto);
        SystemConfigDTO r = systemConfigService.findSystemConfigDTOById(created.id());
        assertThat(r.configKey()).isEqualTo("FIND_KEY");
    }

    @Test @DisplayName("查询 — 按 key 获取值")
    void getConfigValue() {
        SystemConfigCreateDTO dto = new SystemConfigCreateDTO("VAL_KEY", "hello", "测试", "GRP");
        systemConfigService.saveSystemConfig(dto);
        String val = systemConfigService.getConfigValue("VAL_KEY");
        assertThat(val).isEqualTo("hello");
    }

    @Test @DisplayName("查询 — 按分组获取")
    void getByGroup() {
        SystemConfigCreateDTO dto = new SystemConfigCreateDTO("GRP_KEY", "v", "测试", "MY_GROUP");
        systemConfigService.saveSystemConfig(dto);
        List<SystemConfigDTO> list = systemConfigService.getConfigsByGroup("MY_GROUP");
        assertThat(list).isNotEmpty();
    }

    @Test @DisplayName("查询 — 分组转 Map")
    void getConfigsAsMap() {
        SystemConfigCreateDTO dto = new SystemConfigCreateDTO("MAP_KEY", "map_val", "测试", "MAP_GRP");
        systemConfigService.saveSystemConfig(dto);
        Map<String, String> map = systemConfigService.getConfigsAsMap("MAP_GRP");
        assertThat(map).containsEntry("MAP_KEY", "map_val");
    }

    @Test @DisplayName("更新配置")
    void update() {
        SystemConfigCreateDTO dto = new SystemConfigCreateDTO("UPD_KEY", "old", "测试", "GRP");
        SystemConfigDTO created = systemConfigService.saveSystemConfig(dto);
        SystemConfigCreateDTO updateDto = new SystemConfigCreateDTO("UPD_KEY", "new_val", null, null);
        SystemConfigDTO r = systemConfigService.updateSystemConfig(created.id(), updateDto);
        assertThat(r.configValue()).isEqualTo("new_val");
    }

    @Test @DisplayName("updateConfigValue — 直接按 key 更新值")
    void updateConfigValue() {
        SystemConfigCreateDTO dto = new SystemConfigCreateDTO("DIR_KEY", "old", "测试", "GRP");
        systemConfigService.saveSystemConfig(dto);
        SystemConfigDTO r = systemConfigService.updateConfigValue("DIR_KEY", "new_direct");
        assertThat(r.configValue()).isEqualTo("new_direct");
    }

    @Test @DisplayName("软删除")
    void delete() {
        SystemConfigCreateDTO dto = new SystemConfigCreateDTO("DEL_KEY", "val", "测试", "GRP");
        SystemConfigDTO created = systemConfigService.saveSystemConfig(dto);
        systemConfigService.deleteById(created.id());
    }
}
