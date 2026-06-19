package com.henashi.inventorycrm;

import com.henashi.inventorycrm.pojo.InventoryLog;
import com.henashi.inventorycrm.pojo.Product;
import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.repository.InventoryLogRepository;
import com.henashi.inventorycrm.repository.ProductRepository;
import com.henashi.inventorycrm.repository.UserRepository;
import com.henashi.inventorycrm.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class InventoryOperationRegressionTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void stockInPersistsTheSubmittedReasonInsteadOfUsingAHardcodedLabel() throws IOException, InterruptedException {
        Product product = productRepository.saveAndFlush(Product.builder()
                .name(uniqueValue("入库商品"))
                .code(uniqueValue("PROD"))
                .price(BigDecimal.TEN)
                .currentStock(3)
                .safeStock(5)
                .unit("件")
                .status("1")
                .build());
        String token = jwtService.generateToken(createUser("mgr", "MANAGER"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/inventories/in"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "productId": %d,
                          "quantity": 5,
                          "reason": "补货到仓",
                          "remark": "618备货"
                        }
                        """.formatted(product.getId())))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        InventoryLog latestLog = inventoryLogRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .filter(log -> log.getProduct() != null && product.getId().equals(log.getProduct().getId()))
                .findFirst()
                .orElseThrow();
        assertThat(latestLog.getType()).isEqualTo(InventoryLog.LogType.IN);
        assertThat(latestLog.getReason()).isEqualTo("补货到仓");
        assertThat(latestLog.getRemark()).isEqualTo("618备货");
    }

    private User createUser(String prefix, String role) {
        return userRepository.save(User.builder()
                .username(uniqueValue(prefix))
                .password("secret")
                .realName(prefix)
                .email(uniqueValue("u") + "@e.com")
                .role(role)
                .status("1")
                .tokenVersion(0)
                .build());
    }

    private String uniqueValue(String prefix) {
        return prefix + System.nanoTime();
    }
}
