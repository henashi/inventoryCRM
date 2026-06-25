package com.henashi.inventorycrm.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM API 调用服务
 * <p>
 * 支持 DeepSeek API，无 Key 时自动降级到关键词匹配。
 */
@Slf4j
@Service
public class LLMService {

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.provider:deepseek}")
    private String provider;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions";
    private static final String SYSTEM_PROMPT = """
            你是一个库存CRM系统的AI助手。你的任务是将用户的中文问题解析为结构化JSON。

            支持以下意图类型：
            1. stock_query - 库存查询（如"哪些商品库存不足？"）
            2. customer_query - 客户查询（如"最近新增的客户"）
            3. gift_query - 礼品查询（如"还有哪些礼品可以发放"）
            4. stat_query - 统计查询（如"上月出库总量"）
            5. general_chat - 普通对话（如"你好"）

            请严格按以下JSON格式返回，不要包含其他文字：
            {"intent":"意图类型","params":{...},"requiresQuery":true/false}
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LLMService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用 LLM 解析意图
     *
     * @param userMessage 用户消息
     * @return 解析后的意图 JSON 字符串
     */
    public String analyzeIntent(String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("AI API Key 未配置，使用降级关键词匹配");
            return fallbackIntentAnalysis(userMessage);
        }
        return callDeepSeek(userMessage);
    }

    /**
     * 调用 LLM 生成自然语言回答
     *
     * @param systemPrompt 系统提示
     * @param data 查询结果数据
     * @param userQuery 用户原问题
     * @return 自然语言回答
     */
    public String generateAnswer(String systemPrompt, String data, String userQuery) {
        if (apiKey == null || apiKey.isBlank()) {
            // 降级：直接返回数据
            return data;
        }
        return callDeepSeekWithPrompt(systemPrompt, data, userQuery);
    }

    // ==================== DeepSeek API 调用 ====================

    private String callDeepSeek(String userMessage) {
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 500);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
            messages.add(Map.of("role", "user", "content", userMessage));
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // 验证是有效 JSON
            objectMapper.readTree(content);
            return content;
        } catch (Exception e) {
            log.warn("DeepSeek API 调用失败: {}, 使用降级模式", e.getMessage());
            return fallbackIntentAnalysis(userMessage);
        }
    }

    private String callDeepSeekWithPrompt(String systemPrompt, String data, String userQuery) {
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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(DEEPSEEK_API_URL, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.warn("DeepSeek API 调用失败: {}", e.getMessage());
            return data;
        }
    }

    // ==================== 降级：关键词匹配 ====================

    String fallbackIntentAnalysis(String message) {
        String msg = message.toLowerCase();

        if (msg.contains("库存不足") || msg.contains("缺货") || msg.contains("低库存")
                || (msg.contains("库存") && (msg.contains("哪些") || msg.contains("什么")))) {
            return "{\"intent\":\"stock_query\",\"params\":{\"lowStockOnly\":true},\"requiresQuery\":true}";
        }
        if (msg.contains("入库") || (msg.contains("出库") && !msg.contains("最多") && !msg.contains("统计"))) {
            String type = msg.contains("入库") ? "IN" : "OUT";
            return "{\"intent\":\"stock_query\",\"params\":{\"logType\":\"" + type + "\"},\"requiresQuery\":true}";
        }
        if (msg.contains("新增客户") || msg.contains("最近客户") || (msg.contains("客户") && msg.contains("新增"))) {
            return "{\"intent\":\"customer_query\",\"params\":{\"recentFirst\":true},\"requiresQuery\":true}";
        }
        if (msg.contains("礼品") || msg.contains("赠品")) {
            boolean active = msg.contains("可以") || msg.contains("可用") || msg.contains("能发");
            return "{\"intent\":\"gift_query\",\"params\":{\"activeOnly\":" + active + "},\"requiresQuery\":true}";
        }
        if ((msg.contains("出库") || msg.contains("入库")) && (msg.contains("最多") || msg.contains("统计") || msg.contains("总量"))) {
            String type = msg.contains("入库") ? "IN" : "OUT";
            return "{\"intent\":\"stat_query\",\"params\":{\"statType\":\"total_" + type.toLowerCase() + "\"},\"requiresQuery\":true}";
        }
        if (msg.contains("你好") || msg.contains("帮助") || msg.contains("hello") || msg.contains("hi")) {
            return "{\"intent\":\"general_chat\",\"params\":{},\"requiresQuery\":false}";
        }
        // 默认
        return "{\"intent\":\"general_chat\",\"params\":{\"suggestion\":\"stock_query\"},\"requiresQuery\":false}";
    }
}
