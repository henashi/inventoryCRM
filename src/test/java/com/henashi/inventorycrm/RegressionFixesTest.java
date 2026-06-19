package com.henashi.inventorycrm;

import com.henashi.inventorycrm.dto.CustomerBatchStatusUpdateDTO;
import com.henashi.inventorycrm.dto.CustomerDTO;
import com.henashi.inventorycrm.dto.UpdateProfileRequest;
import com.henashi.inventorycrm.dto.UserDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.pojo.Customer;
import com.henashi.inventorycrm.pojo.Gift;
import com.henashi.inventorycrm.pojo.GiftLog;
import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.repository.CustomerRepository;
import com.henashi.inventorycrm.repository.GiftLogRepository;
import com.henashi.inventorycrm.repository.GiftRepository;
import com.henashi.inventorycrm.repository.UserRepository;
import com.henashi.inventorycrm.service.AuthService;
import com.henashi.inventorycrm.service.CustomerService;
import com.henashi.inventorycrm.service.GiftLogService;
import com.henashi.inventorycrm.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class RegressionFixesTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private GiftLogService giftLogService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private GiftRepository giftRepository;

    @Autowired
    private GiftLogRepository giftLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CacheManager cacheManager;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        clearCache("customers");
        clearCache("giftLogs");
    }

    @Test
    void updateProfileRejectsUsernameChangeButStillKeepsCurrentUsernameForSuccessfulProfileUpdates() {
        User user = userRepository.save(User.builder()
                .username(uniqueValue("profile-user"))
                .password("secret")
                .realName("原姓名")
                .email(uniqueValue("profile") + "@example.com")
                .role("USER")
                .status("1")
                .tokenVersion(0)
                .build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities())
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.updateProfile(
                UpdateProfileRequest.builder()
                        .username(uniqueValue("renamed-user"))
                        .realName("新姓名")
                        .email(uniqueValue("renamed") + "@example.com")
                        .build()
        ));

        assertThat(exception.getMessage()).contains("用户名");
        User unchangedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(unchangedUser.getUsername()).isEqualTo(user.getUsername());
        assertThat(unchangedUser.getRealName()).isEqualTo("原姓名");
        assertThat(unchangedUser.getEmail()).isEqualTo(user.getEmail());

        UserDTO updatedProfile = authService.updateProfile(UpdateProfileRequest.builder()
                .username(user.getUsername())
                .realName("已更新姓名")
                .email(uniqueValue("updated") + "@example.com")
                .build());

        assertThat(updatedProfile.username()).isEqualTo(user.getUsername());
        assertThat(updatedProfile.realName()).isEqualTo("已更新姓名");
        assertThat(updatedProfile.email()).isEqualTo(userRepository.findById(user.getId()).orElseThrow().getEmail());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void inventoriesEndpointsRequireManagerOrAdminRole() throws IOException, InterruptedException {
        String userToken = jwtService.generateToken(createUser("inventory-user", "USER"));
        String managerToken = jwtService.generateToken(createUser("inventory-manager", "MANAGER"));

        assertThat(getInventoriesStatusCode(userToken)).isEqualTo(403);
        assertThat(getInventoriesStatusCode(managerToken)).isEqualTo(200);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void customersListEndpointBindsExplicitRequestParams() throws IOException, InterruptedException {
        String userToken = jwtService.generateToken(createUser("customer-user", "USER"));

        assertThat(getCustomersStatusCode(userToken)).isEqualTo(200);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void giftLogsListEndpointBindsExplicitRequestParams() throws IOException, InterruptedException {
        String userToken = jwtService.generateToken(createUser("gift-log-user", "USER"));

        assertThat(getGiftLogsStatusCode(userToken)).isEqualTo(200);
    }

    @Test
    void batchUpdateStatusEvictsCustomerDetailCache() {
        Customer customer = customerRepository.save(Customer.builder()
                .name("缓存客户")
                .phone(uniquePhone("138"))
                .email(uniqueValue("customer") + "@example.com")
                .status("1")
                .build());

        CustomerDTO beforeUpdate = customerService.findCustomerDTOById(customer.getId());
        Cache customerCache = getCache("customers");
        assertThat(beforeUpdate.status()).isEqualTo(1);
        assertThat(customerCache.get(customer.getId())).isNotNull();

        customerService.batchUpdateStatus(new CustomerBatchStatusUpdateDTO(List.of(customer.getId()), 0));

        assertThat(customerCache.get(customer.getId())).isNull();
        CustomerDTO afterUpdate = customerService.findCustomerDTOById(customer.getId());
        assertThat(afterUpdate.status()).isEqualTo(0);
    }

    @Test
    void deleteGiftLogEvictsGiftLogDetailCache() {
        Customer customer = customerRepository.save(Customer.builder()
                .name("礼品日志客户")
                .phone(uniquePhone("139"))
                .email(uniqueValue("giftlog-customer") + "@example.com")
                .status("1")
                .build());
        Gift gift = giftRepository.save(Gift.builder()
                .name(uniqueValue("礼品"))
                .code(uniqueValue("GIFT"))
                .type(Gift.GiftType.PHYSICAL)
                .limitEnabled(false)
                .giftStatus(Gift.GiftStatus.ACTIVE)
                .build());
        GiftLog giftLog = giftLogRepository.save(GiftLog.builder()
                .customer(customer)
                .gift(gift)
                .quantity(1)
                .operator("system")
                .giftLogStatus(GiftLog.GiftLogStatus.ISSUED)
                .build());

        assertThat(giftLogService.findGiftLogDTOById(giftLog.getId()).id()).isEqualTo(giftLog.getId());
        Cache giftLogCache = getCache("giftLogs");
        assertThat(giftLogCache.get(giftLog.getId())).isNotNull();

        giftLogService.deleteGiftLog(giftLog.getId());

        assertThat(giftLogCache.get(giftLog.getId())).isNull();
        assertThat(giftLogRepository.findById(giftLog.getId())).isEmpty();
    }

    private Cache getCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();
        return cache;
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private int getInventoriesStatusCode(String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/inventories"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private int getCustomersStatusCode(String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/customers?page=0&size=5"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private int getGiftLogsStatusCode(String token) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/gift-logs?page=0&size=10"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private User createUser(String prefix, String role) {
        return userRepository.save(User.builder()
                .username(uniqueValue(prefix))
                .password("secret")
                .realName(prefix)
                .email(uniqueValue(prefix) + "@example.com")
                .role(role)
                .status("1")
                .tokenVersion(0)
                .build());
    }

    private String uniqueValue(String prefix) {
        return prefix + System.nanoTime();
    }

    private String uniquePhone(String prefix) {
        String digits = String.valueOf(System.nanoTime());
        String suffix = digits.substring(Math.max(0, digits.length() - 8));
        return prefix + suffix;
    }
}
