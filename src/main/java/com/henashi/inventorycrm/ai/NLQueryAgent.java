package com.henashi.inventorycrm.ai;

import com.henashi.inventorycrm.ai.dto.ChatRequestDTO;
import com.henashi.inventorycrm.ai.dto.ChatRequestDTO.ChatMessage;
import com.henashi.inventorycrm.ai.dto.ChatResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
     * 流式处理用户消息 — SSE 逐事件推送
     * <p>
     * 事件流：
     *   event: status, data: "正在分析意图…"
     *   event: intent, data: {intentJson}
     *   event: status, data: "正在查询数据…"
     *   event: status, data: "正在生成回答…"
     *   event: token, data: "单 token"
     *   event: token, data: "下一个 token"
     *   event: done, data: {"fallback":false}
     */
    public void processStream(ChatRequestDTO request, SseEmitter emitter) {
        List<ChatMessage> history = request.history() != null ? request.history() : List.of();
        String userMessage = request.message();

        CompletableFuture.runAsync(() -> {
            try {
                // Step 1: 意图分析
                sendEvent(emitter, "status", "正在理解你的问题…");
                String contextSummary = buildContextSummary(history);
                String contextualMessage = contextSummary.isEmpty()
                        ? userMessage
                        : contextSummary + "\n当前问题: " + userMessage;

                String intentJson = llmService.analyzeIntent(contextualMessage);
                sendEvent(emitter, "intent", intentJson);

                // Step 2: 查询数据库
                boolean requiresQuery = intentJson.contains("\"requiresQuery\":true")
                        || intentJson.contains("\"requiresQuery\": true");

                String queryResult;
                if (requiresQuery) {
                    sendEvent(emitter, "status", "正在查询数据…");
                    queryResult = queryExecutor.execute(intentJson);
                } else {
                    queryResult = null;
                }

                // Step 3: 流式生成回答
                sendEvent(emitter, "status", "正在生成回答…");

                final boolean[] isFallback = {false};

                if (queryResult != null) {
                    String prompt = String.format(ANSWER_PROMPT, queryResult);
                    llmService.streamAnswer(prompt, queryResult, userMessage,
                            token -> sendEvent(emitter, "token", token),
                            () -> { /* done — 下面发送 done 事件 */ }
                    );
                } else {
                    isFallback[0] = true;
                    String fallbackReply = "你可以这样问我：\n"
                            + "• 咱们仓库有什么商品？\n"
                            + "• 哪些商品库存不足？\n"
                            + "• 最近新增了哪些客户？\n"
                            + "• 有哪些礼品可以发放？";
                    sendEvent(emitter, "token", fallbackReply);
                }

                // Step 4: 完成
                sendEvent(emitter, "done", "{\"fallback\":false}");

            } catch (Exception e) {
                log.error("SSE 流式处理异常", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("处理请求时出错，请稍后重试。"));
                    emitter.send(SseEmitter.event()
                            .name("done")
                            .data("{\"fallback\":true}"));
                } catch (IOException ex) {
                    log.debug("发送 SSE 错误事件失败（连接可能已关闭）: {}", ex.getMessage());
                }
            } finally {
                try {
                    emitter.complete();
                } catch (Exception ex) {
                    log.debug("关闭 SSE 连接时异常: {}", ex.getMessage());
                }
            }
        });
    }

    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.warn("SSE 发送事件失败 ({}): {}", eventName, e.getMessage());
            // emitter 已断开，让 CompletableFuture 自然结束
        }
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
