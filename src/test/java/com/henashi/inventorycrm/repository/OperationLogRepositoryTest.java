package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.OperationLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OperationLogRepositoryTest {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Test
    @DisplayName("search — 关键词搜索操作日志")
    void search() {
        // given: 创建几条操作日志
        OperationLog log1 = new OperationLog();
        log1.setModule("商品管理");
        log1.setDescription("创建商品 A");
        log1.setOperator("admin");
        operationLogRepository.save(log1);

        OperationLog log2 = new OperationLog();
        log2.setModule("客户管理");
        log2.setDescription("更新客户资料");
        log2.setOperator("manager");
        operationLogRepository.save(log2);

        // when: 搜索关键词 "商品"
        Page<OperationLog> results = operationLogRepository.search("商品", PageRequest.of(0, 10));

        // then: 应匹配 module 和 description
        assertFalse(results.isEmpty());
        boolean matchModule = results.getContent().stream().anyMatch(r -> "商品管理".equals(r.getModule()));
        boolean matchDescription = results.getContent().stream().anyMatch(r -> r.getDescription() != null && r.getDescription().contains("商品"));
        assertTrue(matchModule || matchDescription);
    }
}
