package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.DataDictCreateDTO;
import com.henashi.inventorycrm.dto.DataDictDTO;
import com.henashi.inventorycrm.dto.DataDictUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DataDictServiceTest {

    @Autowired private DataDictService dataDictService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test @DisplayName("创建配置项")
    void create() {
        DataDictCreateDTO dto = new DataDictCreateDTO("NEW_KEY", "新键", "new_val", "TEST_GROUP", "测试组", "测试");
        DataDictDTO r = dataDictService.createDataDict(dto);
        assertThat(r.id()).isNotNull();
        assertThat(r.paramCode()).isEqualTo("NEW_KEY");
    }

    @Test @DisplayName("分页查询")
    void list() {
        Page<DataDictDTO> page = dataDictService.findDataDictPage(PageRequest.of(0, 10));
        assertThat(page).isNotNull();
    }

    @Test @DisplayName("更新配置项 — 先创建再更新")
    void update() {
        DataDictCreateDTO createDto = new DataDictCreateDTO("UPD_KEY", "原名", "原值", "UPD_GRP", "测试", null);
        DataDictDTO created = dataDictService.createDataDict(createDto);
        DataDictUpdateDTO updateDto = new DataDictUpdateDTO(null, "新值", null, null, null, null, null);
        DataDictDTO r = dataDictService.updateDataDict(created.id(), updateDto);
        assertThat(r.paramValue()).isEqualTo("新值");
    }

    @Test @DisplayName("软删除 — 调用成功不抛异常")
    void delete() {
        DataDictCreateDTO createDto = new DataDictCreateDTO("DEL_KEY", "删", "val", "DEL_GRP", "测试", null);
        DataDictDTO created = dataDictService.createDataDict(createDto);
        // 删除不抛异常即为成功（软删除）
        dataDictService.deleteById(created.id());
    }
}
