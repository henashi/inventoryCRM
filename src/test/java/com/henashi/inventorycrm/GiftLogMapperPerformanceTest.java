package com.henashi.inventorycrm;

import com.henashi.inventorycrm.dto.GiftLogCreateDTO;
import com.henashi.inventorycrm.dto.GiftLogDTO;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.GiftLogRepository;
import com.henashi.inventorycrm.repository.GiftRepository;
import com.henashi.inventorycrm.service.GiftLogService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@Transactional
@SpringBootTest
class GiftLogMapperPerformanceTest {

    @Autowired
    private GiftLogService giftLogService;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private GiftRepository giftRepository;

    @MockitoBean
    private GiftLogRepository giftLogRepository;

    @BeforeEach
    void setUp() {
        // 模拟数据
        Customer customer = Customer.builder()
                .id(2L)
                .name("测试客户")
                .build();

        Gift gift = Gift.builder()
                .id(2L)
                .name("测试礼品")
                .build();

        when(customerRepository.findById(2L))
                .thenReturn(Optional.of(customer));
        when(giftRepository.findById(2L))
                .thenReturn(Optional.of(gift));
        when(giftLogRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @RepeatedTest(10)  // 只测试10次
    void testMapperWithCachePerformance(RepetitionInfo repetitionInfo) {
        long start = System.nanoTime();  // 使用纳秒更精确

        GiftLogCreateDTO dto = new GiftLogCreateDTO(2L, 2L, "system", null, 1, null);

        // 这里只是测试Mapper转换，不保存到数据库
        GiftLogDTO giftLog = giftLogService.saveGiftLog(dto);

        long duration = System.nanoTime() - start;
        long durationMs = duration / 1_000_000;

        log.info("Mapper转换耗时: {}ns ({}ms)", duration, durationMs);
        // 🔥 忽略第一次预热，只断言后面的缓存命中情况
        if (repetitionInfo.getCurrentRepetition() > 1) {
            assertTrue(durationMs < 20, "缓存命中后应在20ms内完成");
        }
    }
}