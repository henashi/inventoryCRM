package com.henashi.inventorycrm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.henashi.inventorycrm.pojo.User;
import com.henashi.inventorycrm.security.JwtAccessDeniedHandler;
import com.henashi.inventorycrm.security.JwtAuthenticationEntryPoint;
import com.henashi.inventorycrm.security.JwtAuthenticationFilter;
import com.henashi.inventorycrm.service.UserDetailsServiceImpl;
import com.henashi.inventorycrm.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class WebSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner createDefaultUsers(UserService userService) {
        return args -> {
            // 只在开发环境或明确启用时创建默认用户
            if (shouldCreateDefaultUsers()) {
                createUserIfNotExists("admin", "admin123", "Admin", userService);
                createUserIfNotExists("user", "user123", "User", userService);
                createUserIfNotExists("demo", "demo123", "User", userService);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                // 禁用CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 启用CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 配置会话管理
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 公开接口
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/public/**",
                                "/error",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 静态资源
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/assets/**",
                                "/static/**"
                        ).permitAll()

                        // API接口权限
                        .requestMatchers(HttpMethod.GET, "/api/customers/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/customers").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/customers/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole("ADMIN")

                        .requestMatchers("/api/products/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/inventory/**").hasAnyRole("MANAGER", "ADMIN")

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 默认需要认证
                        .anyRequest().authenticated()
                )

                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)

                // 异常处理
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new JwtAccessDeniedHandler())
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的前端地址
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:8081",  // Vue开发服务器
                "http://localhost:8080"   // 生产部署
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With",
                "Accept", "Origin", "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Disposition"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity, UserDetailsServiceImpl userService) throws Exception{
        AuthenticationManagerBuilder authenticationManagerBuilder = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userService)
                .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    private void createUserIfNotExists(String username, String plainPassword,
                                       String role, UserService userService) {

        if (userService.getUserByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(plainPassword);
            user.setEmail(username + "@quizapp.com");
            user.setRole(role);
            user.setStatus(1);
            try {
                userService.registerUser(user);
            }
            catch (Exception e) {
                log.warn("Add default user fail");
            }
            log.info("Create default User success:" + username + "(role: "+ role + ")");
        }
    }

    private boolean shouldCreateDefaultUsers() {
        // 可以根据环境变量或其他条件判断
        return true; // 或者从配置读取
    }
}
