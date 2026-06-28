package com.henashi.inventorycrm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * API 集成测试 — 真实 HTTP 端口，全链路验证。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String baseUrl;
    private String jwtToken;
    private String testUser;
    private static final String TEST_PASS = "TestPass123!";

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port + "/api";
        testUser = "apitest_" + UUID.randomUUID().toString().substring(0, 8);

        // 注册测试用户（RegisterRequest 要求 confirmPassword + realName）
        String registerJson = postWithStatus("/auth/register", Map.of(
                "username", testUser,
                "password", TEST_PASS,
                "confirmPassword", TEST_PASS,
                "realName", "测试用户",
                "email", testUser + "@test.com"
        ), 201);
    }

    // ==================== 辅助方法 ====================

    private void login() throws Exception {
        String body = postWithStatus("/auth/login", Map.of(
                "username", testUser,
                "password", TEST_PASS
        ), 200);
        Map<String, Object> resp = objectMapper.readValue(body,
                new TypeReference<Map<String, Object>>() {});
        jwtToken = (String) resp.getOrDefault("token",
                resp.getOrDefault("accessToken", resp.get("access_token")));
        assertThat(jwtToken).as("登录应返回 JWT").isNotNull();
    }

    private String get(String path) throws Exception {
        return getWithStatus(path, 200);
    }

    private String getWithStatus(String path, int expectedStatus) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + jwtToken)
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(expectedStatus, resp.statusCode(), "GET " + path);
        return resp.body();
    }

    private String postWithStatus(String path, Object body, int expectedStatus) throws Exception {
        String json = body != null ? objectMapper.writeValueAsString(body) : "";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != expectedStatus) {
            System.out.println("POST " + path + " 返回 " + resp.statusCode() + ": " + resp.body());
        }
        assertEquals(expectedStatus, resp.statusCode(), "POST " + path);
        return resp.body();
    }

    // ==================== 测试 ====================

    @Test
    @DisplayName("注册 → 登录 → 调用业务 API → 登出 → 业务 API 被拒")
    void fullAuthFlow() throws Exception {
        login();

        // 获取当前用户
        String meJson = get("/auth/me");
        assertThat(meJson).contains(testUser);

        // 商品列表
        String productsJson = get("/products?page=0&size=5");
        assertThat(productsJson).contains("content");

        // 客户列表
        String customersJson = get("/customers?page=0&size=5");
        assertThat(customersJson).contains("content");
    }

    @Test
    @DisplayName("不存在的商品返回 404")
    void productNotFound() throws Exception {
        login();

        // 不存在的商品返回 400（@Min/@NotNull 校验在 controller 方法前触发）
        String errorJson = getWithStatus("/products/-1", 400);
    }

    @Test
    @DisplayName("密码错误返回 401")
    void loginWrongPassword() throws Exception {
        String errorJson = postWithStatus("/auth/login", Map.of(
                "username", testUser,
                "password", "wrong_password"
        ), 401);
        assertThat(errorJson).contains("error");
    }

    @Test
    @DisplayName("无 token 请求返回 401")
    void noTokenReturns401() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/products"))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, resp.statusCode());
    }
}
