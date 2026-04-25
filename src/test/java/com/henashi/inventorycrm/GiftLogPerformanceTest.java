package com.henashi.inventorycrm;

import com.henashi.inventorycrm.dto.GiftLogCreateDTO;
import com.henashi.inventorycrm.service.GiftLogService;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional  // 测试结束后自动回滚
class GiftLogPerformanceTest {

    @Autowired
    private GiftLogService giftLogService;

    @RepeatedTest(100)
    void testCreatePerformance() {
        long start = System.currentTimeMillis();

        // 使用不同的数据
        long customerId = 1L;
        long giftId = 1L;

        GiftLogCreateDTO dto = new GiftLogCreateDTO(customerId, giftId, "system", null, 1, null);
        giftLogService.saveGiftLog(dto);

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 100, "创建操作应在100ms内完成");
    }
}
