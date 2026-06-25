package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.ChatRequestDTO;
import com.henashi.inventorycrm.ai.dto.ChatRequestDTO.ChatMessage;
import com.henashi.inventorycrm.ai.dto.ChatResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * 处理用户消息（带历史上下文）
     */
    public ChatResponseDTO processMessage(String userMessage, List<ChatMessage> history) {
        long start = System.currentTimeMillis();

        // Step 1: 意图识别（带历史上下文）
        String contextSummary = buildContextSummary(history);
        String contextualMessage = contextSummary.isEmpty() ? userMessage : contextSummary + "\n当前问题: " + userMessage;
        String intentJson = llmService.analyzeIntent(contextualMessage);
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

        if (queryResult != null) {
            String prompt = String.format(ANSWER_PROMPT, queryResult);
            reply = llmService.generateAnswer(prompt, queryResult, userMessage);
            fallback = reply.equals(queryResult);
        } else {
            reply = "你可以这样问我：\n"
                    + "• 咱们仓库有什么商品？\n"
                    + "• 哪些商品库存不足？\n"
                    + "• 最近新增了哪些客户？\n"
                    + "• 有哪些礼品可以发放？";
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("NLQueryAgent 处理完成: 耗时 {}ms, 降级={}", elapsed, fallback);

        return new ChatResponseDTO(reply, fallback);
    }

    /**
     * 处理带历史上下文的聊天
     */
    public ChatResponseDTO processWithHistory(ChatRequestDTO request) {
        List<ChatMessage> history = request.history();
        return processMessage(request.message(), history != null ? history : List.of());
    }

    /**
     * 构建历史上下文摘要（提取最近几轮的关键信息）
     */
    private String buildContextSummary(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return "";
        // 只取最近 3 轮对话
        int start = Math.max(0, history.size() - 6);
        StringBuilder sb = new StringBuilder("历史对话：\n");
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            String role = "user".equals(msg.role()) ? "用户" : "AI";
            sb.append(role).append(": ").append(msg.content()).append("\n");
        }
        return sb.toString();
    }
}
