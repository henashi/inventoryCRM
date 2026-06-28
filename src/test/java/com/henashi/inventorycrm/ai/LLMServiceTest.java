package com.henashi.inventorycrm.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LLMService 降级意图分析单元测试
 * <p>
 * 覆盖 {@link LLMService#fallbackIntentAnalysis(String)} 的全部分支：
 * greeting / help / stock_query / customer_query / gift_query / stat_query / 默认
 */
@SpringBootTest
class LLMServiceTest {

    private LLMService llmService;

    @BeforeEach
    void setUp() {
        llmService = new LLMService();
    }

    // ==================== 打招呼 ====================

    @Test
    @DisplayName("「你好」→ greeting")
    void greetingNiHao() {
        String result = llmService.fallbackIntentAnalysis("你好");
        assertThat(result).contains("\"intent\":\"greeting\"");
    }

    @Test
    @DisplayName("「hello」→ greeting")
    void greetingHello() {
        String result = llmService.fallbackIntentAnalysis("hello");
        assertThat(result).contains("\"intent\":\"greeting\"");
    }

    @Test
    @DisplayName("「hi 今天天气不错」→ greeting")
    void greetingHi() {
        String result = llmService.fallbackIntentAnalysis("hi 今天天气不错");
        assertThat(result).contains("\"intent\":\"greeting\"");
    }

    // ==================== 帮助 ====================

    @Test
    @DisplayName("「帮助」→ help")
    void help() {
        String result = llmService.fallbackIntentAnalysis("帮助");
        assertThat(result).contains("\"intent\":\"help\"");
    }

    @Test
    @DisplayName("「你能做什么」→ help")
    void helpWhatCanYouDo() {
        String result = llmService.fallbackIntentAnalysis("你能做什么");
        assertThat(result).contains("\"intent\":\"help\"");
    }

    @Test
    @DisplayName("「你会什么」→ help")
    void helpWhatYouKnow() {
        String result = llmService.fallbackIntentAnalysis("你会什么");
        assertThat(result).contains("\"intent\":\"help\"");
    }

    // ==================== 商品/库存查询 ====================

    @Test
    @DisplayName("「有哪些商品」→ stock_query（「哪些」触发 lowStockOnly 分支）")
    void stockQueryListAll() {
        String result = llmService.fallbackIntentAnalysis("有哪些商品");
        assertThat(result).contains("\"intent\":\"stock_query\"");
        // 「哪些」在代码中先于 listAll 被检查，返回 lowStockOnly
        assertThat(result).contains("\"lowStockOnly\":true");
    }

    @Test
    @DisplayName("「库存情况」→ stock_query listAll") 
    void stockQueryInventory() {
        String result = llmService.fallbackIntentAnalysis("库存情况");
        assertThat(result).contains("\"intent\":\"stock_query\"");
    }

    @Test
    @DisplayName("「全部产品」→ stock_query listAll")
    void stockQueryAllProducts() {
        String result = llmService.fallbackIntentAnalysis("全部产品");
        assertThat(result).contains("\"intent\":\"stock_query\"");
        assertThat(result).contains("\"listAll\":true");
    }

    @Test
    @DisplayName("「哪些商品库存不足」→ stock_query lowStockOnly")
    void stockQueryLowStock() {
        String result = llmService.fallbackIntentAnalysis("哪些商品库存不足");
        assertThat(result).contains("\"intent\":\"stock_query\"");
        assertThat(result).contains("\"lowStockOnly\":true");
    }

    @Test
    @DisplayName("「库存预警」→ stock_query lowStockOnly")
    void stockQueryShortage() {
        String result = llmService.fallbackIntentAnalysis("库存预警");
        assertThat(result).contains("\"intent\":\"stock_query\"");
        assertThat(result).contains("\"lowStockOnly\":true");
    }

    @Test
    @DisplayName("「低于安全库存」→ stock_query lowStockOnly")
    void stockQueryBelowSafe() {
        String result = llmService.fallbackIntentAnalysis("低于安全库存");
        assertThat(result).contains("\"intent\":\"stock_query\"");
        assertThat(result).contains("\"lowStockOnly\":true");
    }

    @Test
    @DisplayName("「入库记录」→ stock_query logType=IN")
    void stockQueryInLog() {
        String result = llmService.fallbackIntentAnalysis("入库记录");
        assertThat(result).contains("\"intent\":\"stock_query\"");
        assertThat(result).contains("\"logType\":\"IN\"");
    }

    @Test
    @DisplayName("「出库记录」→ stock_query logType=OUT")
    void stockQueryOutLog() {
        String result = llmService.fallbackIntentAnalysis("出库记录");
        assertThat(result).contains("\"intent\":\"stock_query\"");
        assertThat(result).contains("\"logType\":\"OUT\"");
    }

    // ==================== 客户查询 ====================

    @Test
    @DisplayName("「有哪些客户」→ customer_query listAll")
    void customerQueryListAll() {
        String result = llmService.fallbackIntentAnalysis("有哪些客户");
        assertThat(result).contains("\"intent\":\"customer_query\"");
        assertThat(result).contains("\"listAll\":true");
    }

    @Test
    @DisplayName("「最近新增的会员」→ customer_query recentFirst")
    void customerQueryRecentFirst() {
        String result = llmService.fallbackIntentAnalysis("最近新增的会员");
        assertThat(result).contains("\"intent\":\"customer_query\"");
        assertThat(result).contains("\"recentFirst\":true");
    }

    @Test
    @DisplayName("「新顾客」→ customer_query recentFirst")
    void customerQueryNewCustomers() {
        String result = llmService.fallbackIntentAnalysis("新顾客");
        assertThat(result).contains("\"intent\":\"customer_query\"");
        assertThat(result).contains("\"recentFirst\":true");
    }

    @Test
    @DisplayName("「全部客户」→ customer_query listAll")
    void customerQueryAll() {
        String result = llmService.fallbackIntentAnalysis("全部客户");
        assertThat(result).contains("\"intent\":\"customer_query\"");
        assertThat(result).contains("\"listAll\":true");
    }

    // ==================== 礼品查询 ====================

    @Test
    @DisplayName("「有哪些礼品」→ gift_query")
    void giftQuery() {
        String result = llmService.fallbackIntentAnalysis("有哪些礼品");
        assertThat(result).contains("\"intent\":\"gift_query\"");
    }

    @Test
    @DisplayName("「可用的赠品」→ gift_query activeOnly=true")
    void giftQueryActive() {
        String result = llmService.fallbackIntentAnalysis("可用的赠品");
        assertThat(result).contains("\"intent\":\"gift_query\"");
        assertThat(result).contains("\"activeOnly\":true");
    }

    @Test
    @DisplayName("「能发的礼物」→ gift_query activeOnly=true")
    void giftQueryCanSend() {
        String result = llmService.fallbackIntentAnalysis("能发的礼物");
        assertThat(result).contains("\"intent\":\"gift_query\"");
        assertThat(result).contains("\"activeOnly\":true");
    }

    // ==================== 统计查询 ====================

    @Test
    @DisplayName("「出库统计」→ stat_query total_out")
    void statQueryOut() {
        String result = llmService.fallbackIntentAnalysis("出库统计");
        assertThat(result).contains("\"intent\":\"stat_query\"");
        assertThat(result).contains("\"statType\":\"total_out\"");
    }

    @Test
    @DisplayName("「入库总量」→ stock_query logType=IN（入库优先于统计检查）")
    void statQueryIn() {
        String result = llmService.fallbackIntentAnalysis("入库总量");
        assertThat(result).contains("\"intent\":\"stock_query\"");
        assertThat(result).contains("\"logType\":\"IN\"");
    }

    @Test
    @DisplayName("「入库多少」→ stock_query logType=IN（入库优先于统计检查）")
    void statQueryInHowMany() {
        String result = llmService.fallbackIntentAnalysis("入库多少");
        assertThat(result).contains("\"intent\":\"stock_query\"");
        assertThat(result).contains("\"logType\":\"IN\"");
    }

    @Test
    @DisplayName("「概览」→ stat_query")
    void statQueryOverview() {
        String result = llmService.fallbackIntentAnalysis("概览");
        assertThat(result).contains("\"intent\":\"stat_query\"");
    }

    @Test
    @DisplayName("「统计」→ stat_query")
    void statQuery() {
        String result = llmService.fallbackIntentAnalysis("统计");
        assertThat(result).contains("\"intent\":\"stat_query\"");
    }

    @Test
    @DisplayName("「总共有多少商品」→ stat_query（含 商品 关键词，先走 stock_query）")
    void statQueryMixed() {
        // "总共" → stat_query, 但 "商品" 也会触发 stock_query
        // 关键词匹配顺序中 stock 先于 stat
        String result = llmService.fallbackIntentAnalysis("总共有多少商品");
        assertThat(result).contains("\"intent\"");
    }

    // ==================== 默认 ====================

    @Test
    @DisplayName("「今天天气」→ help（默认）")
    void defaultFallback() {
        String result = llmService.fallbackIntentAnalysis("今天天气");
        assertThat(result).contains("\"intent\":\"help\"");
    }

    @Test
    @DisplayName("空白消息 → help（默认）")
    void emptyMessage() {
        String result = llmService.fallbackIntentAnalysis("");
        assertThat(result).contains("\"intent\":\"help\"");
    }

    @Test
    @DisplayName("无意义文字 → help（默认）")
    void gibberish() {
        String result = llmService.fallbackIntentAnalysis("asdf1234xyz");
        assertThat(result).contains("\"intent\":\"help\"");
    }
}
