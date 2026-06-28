package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.*;
import com.henashi.inventorycrm.pojo.Gift;
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
class GiftServiceTest {

    @Autowired private GiftService giftService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test @DisplayName("查询 — 根据 ID")
    void findById() {
        GiftDTO g = giftService.findGiftById(1L);
        assertThat(g.name()).isEqualTo("测试礼品");
    }

    @Test @DisplayName("查询 — 分页列表")
    void listGifts() {
        Page<GiftDTO> page = giftService.findGifts(PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test @DisplayName("创建礼品")
    void createGift() {
        GiftCreateDTO dto = new GiftCreateDTO("新礼品", "NEW_GIFT_001",
                Gift.GiftType.PHYSICAL, 1L, "测试", Gift.GiftStatus.ACTIVE,
                true, 3, null);
        GiftDTO r = giftService.saveGift(dto);
        assertThat(r.id()).isNotNull();
        assertThat(r.name()).isEqualTo("新礼品");
    }

    @Test @DisplayName("更新礼品")
    void updateGift() {
        GiftUpdateDTO dto = new GiftUpdateDTO(null, "已更新", null, null, null, null, null, null, null, null);
        GiftDTO r = giftService.updateGift(1L, dto);
        assertThat(r.name()).isEqualTo("已更新");
    }

    @Test @DisplayName("软删除")
    void deleteGift() {
        giftService.deleteGiftById(1L);
        assertThrows(RuntimeException.class, () -> giftService.findGiftById(1L));
    }

    private void assertThrows(Class<RuntimeException> clazz, Runnable r) {
        try { r.run(); throw new AssertionError("Expected exception"); }
        catch (RuntimeException e) { assertThat(e).isInstanceOf(clazz); }
    }
}
