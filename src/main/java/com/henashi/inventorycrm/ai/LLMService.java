package com.henashi.inventorycrm.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.time.Duration;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM API 调用服务
 * <p>
 * 兼容 OpenAI 协议的大模型 API 调用（DeepSeek / OpenAI / 通义千问等）。
 * 所有供应商参数通过配置注入，代码零耦合。
 * 无 API Key 时自动降级到关键词匹配。
 */
@Slf4j
@Service
public class LLMService {

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.api-url:}")
    private String apiUrl;

    @Value("${ai.model:}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            你是一个库存CRM系统的AI助手。将用户的问题解析为JSON,只能使用以下明确定义的意图和参数:

            1. greeting — 打招呼
               {"intent":"greeting","params":{},"requiresQuery":false}

            2. help — 询问帮助
               {"intent":"help","params":{},"requiresQuery":false}

            3. stock_query — 商品/库存查询
               params可选字段:
               - "lowStockOnly": true   (查询库存不足的商品)
               - "listAll": true        (列出全部商品)
               - "logType": "IN"/"OUT"  (查询出入库记录)
               {"intent":"stock_query","params":{"listAll":true},"requiresQuery":true}

            4. customer_query — 客户查询
               params可选字段:
               - "recentFirst": true    (最近新增)
               - "listAll": true        (全部客户)
               {"intent":"customer_query","params":{"listAll":true},"requiresQuery":true}

            5. gift_query — 礼品查询
               params可选字段:
               - "activeOnly": true     (仅可发放的)
               {"intent":"gift_query","params":{"activeOnly":true},"requiresQuery":true}

            6. stat_query — 统计查询
               params可选字段:
               - "statType": "total_in"/"total_out" (入库/出库统计)
               {"intent":"stat_query","params":{},"requiresQuery":true}

            只返回JSON,不要包含其他文字。
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LLMService() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用 LLM 解析意图
     */
    @CircuitBreaker(name = "llmService", fallbackMethod = "analyzeIntentFallback")
    @RateLimiter(name = "llmService")
    public String analyzeIntent(String userMessage) {
        if (!isConfigured()) {
            log.info("AI API 未配置（缺 api-key/url/model），使用降级关键词匹配");
            return fallbackIntentAnalysis(userMessage);
        }
        return callLLM(userMessage);
    }

    /**
     * CircuitBreaker 降级 — 意图解析
     */
    private String analyzeIntentFallback(String userMessage, Throwable t) {
        log.warn("CircuitBreaker 触发 analyzeIntent 降级: {}", t.getMessage());
        return fallbackIntentAnalysis(userMessage);
    }

    /**
     * 调用 LLM 生成自然语言回答
     */
    @CircuitBreaker(name = "llmService", fallbackMethod = "generateAnswerFallback")
    @RateLimiter(name = "llmService")
    public String generateAnswer(String systemPrompt, String data, String userQuery) {
        if (!isConfigured()) {
            return data;
        }
        return callLLMWithPrompt(systemPrompt, data, userQuery);
    }

    /**
     * CircuitBreaker 降级 — 回答生成
     */
    private String generateAnswerFallback(String systemPrompt, String data, String userQuery, Throwable t) {
        log.warn("CircuitBreaker 触发 generateAnswer 降级: {}", t.getMessage());
        return data;
    }

    /**
     * 调用 LLM 生成回答 — SSE 流式版本
     * <p>
     * 逐 token 通过 onToken 回调推送。降级模式直接推送全部内容。
     */
    @RateLimiter(name = "llmService")
    public void streamAnswer(String systemPrompt, String data, String userQuery,
                             Consumer<String> onToken, Runnable onDone) {
        if (!isConfigured()) {
            // 降级模式：全文作为单个 token 发送
            onToken.accept(data != null ? data : "没有找到相关信息。");
            onDone.run();
            return;
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("stream", true);
            requestBody.put("temperature", 0.5);
            requestBody.put("max_tokens", 2000);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content",
                    "问题：" + userQuery + "\n数据：" + (data != null ? data : "")));
            requestBody.put("messages", messages);

            byte[] bodyBytes = objectMapper.writeValueAsBytes(requestBody);

            HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
                os.flush();
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                log.warn("LLM API 流式调用返回 {}，降级到同步模式", status);
                // 降级到已有的同步方法
                String fullReply = callLLMWithPrompt(systemPrompt, data, userQuery);
                onToken.accept(fullReply);
                onDone.run();
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int tokenCount = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String payload = line.substring(6).trim();
                        if ("[DONE]".equals(payload)) break;
                        if (payload.isEmpty()) continue;
                        try {
                            JsonNode json = objectMapper.readTree(payload);
                            JsonNode delta = json.path("choices").get(0).path("delta").path("content");
                            if (!delta.isMissingNode()) {
                                String token = delta.asText();
                                if (!token.isEmpty()) {
                                    onToken.accept(token);
                                    tokenCount++;
                                }
                            }
                        } catch (Exception e) {
                            log.debug("SSE 行解析跳过: {}", e.getMessage());
                        }
                    }
                }
                if (tokenCount == 0) {
                    log.warn("LLM 流式返回无有效 token，使用备用同步调用");
                    String fullReply = callLLMWithPrompt(systemPrompt, data, userQuery);
                    onToken.accept(fullReply);
                }
            }
            onDone.run();
        } catch (Exception e) {
            log.warn("LLM API 流式调用异常: {}, 降级到同步模式", e.getMessage());
            try {
                String fullReply = callLLMWithPrompt(systemPrompt, data, userQuery);
                onToken.accept(fullReply);
            } catch (Exception ex) {
                onToken.accept(data != null ? data : "查询完成，但生成回答时出错。");
            }
            onDone.run();
        }
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && apiUrl != null && !apiUrl.isBlank()
                && model != null && !model.isBlank();
    }

    // ==================== LLM API 调用 ====================

    private String callLLM(String userMessage) {
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 500);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", userMessage));
            requestBody.put("messages", messages);

            String reply = doPost(requestBody);
            objectMapper.readTree(reply); // 验证是有效 JSON
            return reply;
        } catch (Exception e) {
            log.warn("LLM API 意图解析失败: {}, 使用降级模式", e.getMessage());
            return fallbackIntentAnalysis(userMessage);
        }
    }

    private String callLLMWithPrompt(String systemPrompt, String data, String userQuery) {
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.5);
            requestBody.put("max_tokens", 1000);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content",
                    "问题：" + userQuery + "\n数据：" + data));
            requestBody.put("messages", messages);

            return doPost(requestBody);
        } catch (Exception e) {
            log.warn("LLM API 回答生成失败: {}", e.getMessage());
            return data;
        }
    }

    private String doPost(Map<String, Object> requestBody) {
        long start = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            log.debug("LLM API 同步调用完成，耗时 {}ms", System.currentTimeMillis() - start);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            if (e.getCause() instanceof java.net.SocketTimeoutException) {
                log.error("LLM API 请求超时（已等待 {}ms）: {}", System.currentTimeMillis() - start, apiUrl);
                throw new RuntimeException("LLM API 请求超时，请检查网络或 API 服务状态", e);
            }
            log.error("LLM API 网络异常（耗时 {}ms）: {}", System.currentTimeMillis() - start, e.getMessage());
            throw new RuntimeException("LLM API 网络异常: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("LLM API 调用失败（耗时 {}ms）: {}", System.currentTimeMillis() - start, e.getMessage());
            throw new RuntimeException("LLM API 调用失败: " + e.getMessage(), e);
        }
    }

    // ==================== 降级：关键词匹配 ====================

    String fallbackIntentAnalysis(String message) {
        String msg = message.toLowerCase();

        // ===== 打招呼 =====
        if (msg.contains("你好") || msg.contains("hello") || msg.contains("hi")) {
            return "{\"intent\":\"greeting\",\"params\":{},\"requiresQuery\":false}";
        }
        if (msg.contains("帮助") || msg.contains("能做什么") || msg.contains("你会什么")) {
            return "{\"intent\":\"help\",\"params\":{},\"requiresQuery\":false}";
        }

        // ===== 商品/库存查询 =====
        boolean stockQuestion = msg.contains("商品") || msg.contains("库存") || msg.contains("仓库") || msg.contains("存货") || msg.contains("产品");
        boolean listAll = msg.contains("分别") || msg.contains("都有") || msg.contains("全部") || msg.contains("所有");
        boolean lowStock = msg.contains("不足") || msg.contains("缺货") || msg.contains("预警") || msg.contains("低于");

        if (stockQuestion) {
            if (listAll) {
                return "{\"intent\":\"stock_query\",\"params\":{\"listAll\":true},\"requiresQuery\":true}";
            }
            if (lowStock || msg.contains("哪些") || msg.contains("什么")) {
                return "{\"intent\":\"stock_query\",\"params\":{\"lowStockOnly\":true},\"requiresQuery\":true}";
            }
            return "{\"intent\":\"stock_query\",\"params\":{\"listAll\":true},\"requiresQuery\":true}";
        }
        if (msg.contains("入库") || (msg.contains("出库") && !msg.contains("统计") && !msg.contains("总量"))) {
            String type = msg.contains("入库") ? "IN" : "OUT";
            return "{\"intent\":\"stock_query\",\"params\":{\"logType\":\"" + type + "\"},\"requiresQuery\":true}";
        }

        // ===== 客户查询 =====
        if (msg.contains("客户") || msg.contains("会员") || msg.contains("顾客")) {
            if (msg.contains("新增") || msg.contains("最近") || msg.contains("新")) {
                return "{\"intent\":\"customer_query\",\"params\":{\"recentFirst\":true},\"requiresQuery\":true}";
            }
            if (listAll) {
                return "{\"intent\":\"customer_query\",\"params\":{\"listAll\":true},\"requiresQuery\":true}";
            }
            return "{\"intent\":\"customer_query\",\"params\":{\"listAll\":true},\"requiresQuery\":true}";
        }

        // ===== 礼品查询 =====
        if (msg.contains("礼品") || msg.contains("赠品") || msg.contains("礼物")) {
            boolean active = msg.contains("可以") || msg.contains("可用") || msg.contains("能发");
            return "{\"intent\":\"gift_query\",\"params\":{\"activeOnly\":" + active + "},\"requiresQuery\":true}";
        }

        // ===== 统计查询 =====
        if ((msg.contains("出库") || msg.contains("入库")) && (msg.contains("统计") || msg.contains("总量") || msg.contains("多少") || msg.contains("最多"))) {
            String type = msg.contains("入库") ? "IN" : "OUT";
            return "{\"intent\":\"stat_query\",\"params\":{\"statType\":\"total_" + type.toLowerCase() + "\"},\"requiresQuery\":true}";
        }
        if (msg.contains("统计") || msg.contains("总共有") || msg.contains("概览")) {
            return "{\"intent\":\"stat_query\",\"params\":{},\"requiresQuery\":true}";
        }

        // ===== 默认 =====
        return "{\"intent\":\"help\",\"params\":{},\"requiresQuery\":false}";
    }
}
