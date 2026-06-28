package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.OperationLogCreateDTO;
import com.henashi.inventorycrm.dto.OperationLogDTO;
import com.henashi.inventorycrm.enums.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OperationLogServiceTest {

    @Autowired private OperationLogService operationLogService;

    private Long logId;

    @BeforeEach
    void setUp() {
        OperationLogCreateDTO dto = new OperationLogCreateDTO(
                "TEST_MODULE", OperationType.CREATE, "测试操作日志",
                "/api/test", "POST", "admin", "127.0.0.1", 1, null, 0L);
        OperationLogDTO saved = operationLogService.saveOperationLog(dto);
        logId = saved.id();
    }

    @Test @DisplayName("创建操作日志")
    void create() {
        assertThat(logId).isNotNull();
    }

    @Test @DisplayName("查询 — 详情")
    void findById() {
        OperationLogDTO r = operationLogService.findOperationLogDTOById(logId);
        assertThat(r.module()).isEqualTo("TEST_MODULE");
    }

    @Test @DisplayName("搜索 — 多条件")
    void searchLogs() {
        Page<OperationLogDTO> page = operationLogService.searchLogs(
                "测试", null, null, null, null, null, PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
    }

    @Test @DisplayName("logOperation — 静默记录")
    void logOperation() {
        OperationLogCreateDTO dto = new OperationLogCreateDTO(
                "AUTO", OperationType.OTHER, "定时任务",
                null, null, "system", null, 1, null, null);
        operationLogService.logOperation(dto);
    }
}
