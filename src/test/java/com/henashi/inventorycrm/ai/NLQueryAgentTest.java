package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.ChatResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * NLQueryAgent 管道连通性测试。
 * Mock LLMService 的外部 API 调用，验证意图→查询→回答的编排。
 */
@SpringBootTest
@Transactional
class NLQueryAgentTest {

    @Autowired
    private NLQueryAgent agent;

    @MockitoBean
    private LLMService llmService;

    @Test
    @DisplayName("greeting 意图 — 无需查库，返回帮助提示")
    void greetingIntent() {
        when(llmService.analyzeIntent(anyString()))
                .thenReturn("{\"intent\":\"greeting\",\"params\":{},\"requiresQuery\":false}");

        ChatResponseDTO result = agent.processMessage("你好", List.of());
        assertThat(result.reply()).contains("你可以这样问我");
    }

    @Test
    @DisplayName("stock_query 意图 — 查库后生成回答")
    void stockQueryIntent() {
        // Mock 意图识别返回 stock_query listAll
        when(llmService.analyzeIntent(anyString()))
                .thenReturn("{\"intent\":\"stock_query\",\"params\":{\"listAll\":true},\"requiresQuery\":true}");
        // Mock 回答生成：直接返回 queryResult 模拟降级
        when(llmService.generateAnswer(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        ChatResponseDTO result = agent.processMessage("有哪些商品", List.of());
        assertThat(result.reply()).contains("测试商品");
        assertThat(result.fallback()).isTrue();
    }

    @Test
    @DisplayName("带历史上下文 — 正常处理")
    void withHistory() {
        when(llmService.analyzeIntent(anyString()))
                .thenReturn("{\"intent\":\"customer_query\",\"params\":{\"listAll\":true},\"requiresQuery\":true}");
        when(llmService.generateAnswer(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        java.util.List<com.henashi.inventorycrm.ai.dto.ChatRequestDTO.ChatMessage> history =
                java.util.List.of(
                        new com.henashi.inventorycrm.ai.dto.ChatRequestDTO.ChatMessage("user", "你好"),
                        new com.henashi.inventorycrm.ai.dto.ChatRequestDTO.ChatMessage("assistant", "你好！我是库存CRM AI助手")
                );
        ChatResponseDTO result = agent.processMessage("有哪些客户", history);
        assertThat(result.reply()).contains("测试客户");
    }

    @Test
    @DisplayName("LLM 返回异常 — 传播异常，由调用方处理")
    void llmError() {
        when(llmService.analyzeIntent(anyString()))
                .thenThrow(new RuntimeException("API 不可达"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> agent.processMessage("你好", List.of()));
    }
}
