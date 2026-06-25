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
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用 LLM 解析意图
     */
    public String analyzeIntent(String userMessage) {
        if (!isConfigured()) {
            log.info("AI API 未配置（缺 api-key/url/model），使用降级关键词匹配");
            return fallbackIntentAnalysis(userMessage);
        }
        return callLLM(userMessage);
    }

    /**
     * 调用 LLM 生成自然语言回答
     */
    public String generateAnswer(String systemPrompt, String data, String userQuery) {
        if (!isConfigured()) {
            return data;
        }
        return callLLMWithPrompt(systemPrompt, data, userQuery);
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
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
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
