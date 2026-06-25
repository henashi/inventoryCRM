package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.ChatRequestDTO;
import com.henashi.inventorycrm.ai.dto.ChatResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI 自然语言查询 Agent
 * <p>
 * 编排意图识别 → 数据查询 → 答案生成的完整闭环。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NLQueryAgent {

    private final LLMService llmService;
    private final QueryExecutor queryExecutor;

    private static final String ANSWER_PROMPT = """
            你是库存CRM系统的AI助手。基于以下查询结果，用中文生成一段自然语言回复。
            要求简洁明了，关键数字突出显示，不要使用Markdown表格。

            查询结果：
            %s
            """;

    /**
     * 处理用户消息
     */
    public ChatResponseDTO processMessage(String userMessage) {
        long start = System.currentTimeMillis();

        // Step 1: 意图识别
        String intentJson = llmService.analyzeIntent(userMessage);
        log.info("意图识别结果: {}", intentJson);

        // Step 2: 判断是否需要查数据库
        boolean requiresQuery = intentJson.contains("\"requiresQuery\":true")
                || intentJson.contains("\"requiresQuery\": true");

        String queryResult;
        if (requiresQuery) {
            queryResult = queryExecutor.execute(intentJson);
        } else {
            queryResult = null;
        }

        // Step 3: 生成回答
        String reply;
        boolean fallback = false;

        if (queryResult != null && !queryResult.startsWith("你好")) {
            // 有查询结果，让 LLM 生成自然语言回答
            String prompt = String.format(ANSWER_PROMPT, queryResult);
            reply = llmService.generateAnswer(prompt, queryResult, userMessage);
            // 如果生成结果就是原始数据（降级模式），标记 fallback
            fallback = reply.equals(queryResult);
        } else if (queryResult != null) {
            reply = queryResult;
        } else {
            reply = "你好！我是库存CRM AI助手。你可以问我：\n"
                    + "• 哪些商品库存不足？\n"
                    + "• 最近新增的客户\n"
                    + "• 有哪些礼品可以发放\n"
                    + "• 上月出库总量是多少";
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("NLQueryAgent 处理完成: 耗时 {}ms, 降级={}", elapsed, fallback);

        return new ChatResponseDTO(reply, fallback);
    }

    /**
     * 处理带历史上下文的聊天（供后续扩展）
     */
    public ChatResponseDTO processWithHistory(ChatRequestDTO request) {
        return processMessage(request.message());
    }
}
