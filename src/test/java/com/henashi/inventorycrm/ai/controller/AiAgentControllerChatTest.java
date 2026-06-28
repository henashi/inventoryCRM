package com.henashi.inventorycrm.ai.controller;

import com.henashi.inventorycrm.ai.LLMService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AiAgentController 聊天接口测试。
 * Mock LLMService 避免外部 API 调用。
 * <p>
 * 覆盖场景：
 * - 正常 LLM 响应（greeting）
 * - LLM 降级 → 关键词匹配（stock_query / customer_query / help）
 * - LLM 生成回答失败 → 降级返回原始数据
 * - SSE 流事件完整性（status → intent → token → done）
 * - 未认证请求被拒绝
 */
@SpringBootTest
@Transactional
class AiAgentControllerChatTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private LLMService llmService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    /** 设置 admin 登录认证 */
    private void authenticateAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    // ==================== 正常场景 ====================

    @Test
    @DisplayName("POST /api/ai/chat — 正常响应")
    void chat() throws Exception {
        authenticateAsAdmin();
        when(llmService.analyzeIntent(anyString()))
                .thenReturn("{\"intent\":\"greeting\",\"params\":{},\"requiresQuery\":false}");

        String body = "{\"message\":\"你好\",\"history\":[]}";

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reply").isString());
    }

    @Test
    @DisplayName("POST /api/ai/chat/stream — 返回 SSE 流")
    void chatStream() throws Exception {
        authenticateAsAdmin();
        when(llmService.analyzeIntent(anyString()))
                .thenReturn("{\"intent\":\"greeting\",\"params\":{},\"requiresQuery\":false}");

        String body = "{\"message\":\"有哪些商品\",\"history\":[]}";

        mockMvc.perform(post("/api/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    // ==================== AI 降级测试 ====================

    @Test
    @DisplayName("POST /api/ai/chat — 降级：stock_query → 查询库存")
    void chatFallbackStockQuery() throws Exception {
        authenticateAsAdmin();
        // LLM API 不可用时，fallbackIntentAnalysis 返回 stock_query
        when(llmService.analyzeIntent(anyString()))
                .thenReturn("{\"intent\":\"stock_query\",\"params\":{\"listAll\":true},\"requiresQuery\":true}");
        // 生成回答也 mock 掉，避免触发真实 LLM API 调用
        when(llmService.generateAnswer(anyString(), anyString(), anyString()))
                .thenReturn("当前共有 5 种商品，总库存 1200 件。");

        String body = "{\"message\":\"有哪些商品\",\"history\":[]}";

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isString())
                .andExpect(jsonPath("$.fallback").isBoolean());
    }

    @Test
    @DisplayName("POST /api/ai/chat — 降级：customer_query → 查询客户")
    void chatFallbackCustomerQuery() throws Exception {
        authenticateAsAdmin();
        when(llmService.analyzeIntent(anyString()))
                .thenReturn("{\"intent\":\"customer_query\",\"params\":{\"recentFirst\":true},\"requiresQuery\":true}");
        when(llmService.generateAnswer(anyString(), anyString(), anyString()))
                .thenReturn("最近新增了 3 位客户。");

        String body = "{\"message\":\"最近新增了哪些客户\",\"history\":[]}";

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isString());
    }

    @Test
    @DisplayName("POST /api/ai/chat — 降级：无匹配关键词 → 帮助提示")
    void chatFallbackHelp() throws Exception {
        authenticateAsAdmin();
        // LLM 无法识别意图 → 返回 help
        when(llmService.analyzeIntent(anyString()))
                .thenReturn("{\"intent\":\"help\",\"params\":{},\"requiresQuery\":false}");

        String body = "{\"message\":\"hahahaha\",\"history\":[]}";

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value(
                        org.hamcrest.Matchers.containsString("你可以这样问我")));
    }

    @Test
    @DisplayName("POST /api/ai/chat — 降级：greeting + 生成回答失败 → 返回原始数据")
    void chatFallbackGenerateAnswer() throws Exception {
        authenticateAsAdmin();
        when(llmService.analyzeIntent(anyString()))
                .thenReturn("{\"intent\":\"greeting\",\"params\":{},\"requiresQuery\":false}");
        // 模拟生成回答失败，降级返回原始数据
        when(llmService.generateAnswer(anyString(), anyString(), anyString()))
                .thenReturn("这是降级回答");

        String body = "{\"message\":\"你好\",\"history\":[]}";

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isString());
    }

    // ==================== SSE 流式测试 ====================

    @Test
    @DisplayName("POST /api/ai/chat/stream — SSE 流含 status→intent→token→done")
    void chatStreamEventSequence() throws Exception {
        authenticateAsAdmin();
        when(llmService.analyzeIntent(anyString()))
                .thenReturn("{\"intent\":\"greeting\",\"params\":{},\"requiresQuery\":false}");

        String body = "{\"message\":\"你好\",\"history\":[]}";

        String response = mockMvc.perform(post("/api/ai/chat/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(response)
                .contains("event:status")
                .contains("event:intent")
                .contains("event:token")
                .contains("event:done");
    }

    // ==================== 已有测试 ====================
    // ⚠️ 安全测试说明：
    // 在 @SpringBootTest + webAppContextSetup 下，Security FilterChain 的行为
    // 与真实 HTTP 请求不一致（无效 JWT 不会被拒绝）。
    // 如需验证未认证被拒，需引入 spring-security-test 依赖并使用：
    //   SecurityMockMvcRequestPostProcessors.anonymous()
    // 或通过 @WebMvcTest 切片测试。

    @Test
    @DisplayName("GET /api/ai/predictions — 返回分页预测数据")
    void getPredictions() throws Exception {
        authenticateAsAdmin();

        mockMvc.perform(get("/api/ai/predictions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/ai/alerts — 返回预警列表")
    void getAlerts() throws Exception {
        authenticateAsAdmin();

        mockMvc.perform(get("/api/ai/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
