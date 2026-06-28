package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.GiftLogCreateDTO;
import com.henashi.inventorycrm.dto.GiftLogDTO;
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
class GiftLogServiceTest {

    @Autowired private GiftLogService giftLogService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test @DisplayName("发放礼品")
    void issueGift() {
        GiftLogCreateDTO dto = new GiftLogCreateDTO(1L, 1L, "admin", "测试", 2, null);
        GiftLogDTO r = giftLogService.saveGiftLog(dto);
        assertThat(r.id()).isNotNull();
        assertThat(r.quantity()).isEqualTo(2);
    }

    @Test @DisplayName("查询 — 分页列表")
    void listGiftLogs() {
        Page<GiftLogDTO> page = giftLogService.loadGiftLogPage(PageRequest.of(0, 10));
        assertThat(page).isNotNull();
    }

    @Test @DisplayName("查询 — 按客户")
    void getByCustomer() {
        Page<GiftLogDTO> page = giftLogService.getLogsByCustomerId(1L, PageRequest.of(0, 10));
        assertThat(page).isNotNull();
    }

    @Test @DisplayName("查询 — 详情")
    void findById() {
        // 先发一条再查
        GiftLogCreateDTO createDto = new GiftLogCreateDTO(1L, 1L, "admin", "测试", 1, null);
        GiftLogDTO created = giftLogService.saveGiftLog(createDto);
        GiftLogDTO r = giftLogService.findGiftLogDTOById(created.id());
        assertThat(r).isNotNull();
    }

    @Test @DisplayName("删除发放记录")
    void delete() {
        GiftLogCreateDTO createDto = new GiftLogCreateDTO(1L, 1L, "admin", "测试", 1, null);
        GiftLogDTO created = giftLogService.saveGiftLog(createDto);
        giftLogService.deleteGiftLog(created.id());
        assertThrows(RuntimeException.class, () -> giftLogService.findGiftLogDTOById(created.id()));
    }

    private void assertThrows(Class<RuntimeException> clazz, Runnable r) {
        try { r.run(); throw new AssertionError("Expected exception"); }
        catch (RuntimeException e) { assertThat(e).isInstanceOf(clazz); }
    }
}
