package com.henashi.inventorycrm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.henashi.inventorycrm.config.properties.AppProperties;
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

    private final AppProperties appProperties;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CommandLineRunner createDefaultUsers(UserService userService) {
        return args -> {
            if (appProperties.isCreateDefaultUsers()) {
                createUserIfNotExists("admin", "admin123", "Admin", userService);
                createUserIfNotExists("user", "user123", "User", userService);
                createUserIfNotExists("demo", "demo123", "User", userService);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh-token"
                        ).permitAll()
                        .requestMatchers(
                                "/api/public/**",
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/assets/**",
                                "/static/**",
                                "/swagger-ui.html",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/auth/profile").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/auth/change-password", "/api/auth/logout").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/customers/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/customers").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/customers/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/gifts", "/api/gifts/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/gifts").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/gifts/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/gifts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/gift-logs", "/api/gift-logs/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/gift-logs").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/gift-logs/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/gift-logs/**").hasRole("ADMIN")
                        .requestMatchers("/api/inventory/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/inventories", "/api/inventories/**").hasAnyRole("MANAGER", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/ai/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new JwtAccessDeniedHandler())
                )
                // ===== 安全响应头 =====
                .headers(headers -> headers
                        .contentTypeOptions(c -> {})   // X-Content-Type-Options: nosniff
                        .frameOptions(frame -> frame.deny())  // X-Frame-Options: DENY
                        .xssProtection(xss -> xss
                                .headerValue(org.springframework.security.web.header.writers
                                        .XXssProtectionHeaderWriter.HeaderValue.DISABLED))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true))
                        .cacheControl(c -> {})          // Cache-Control: no-cache (默认已启用)
                        .addHeaderWriter(new org.springframework.security.web.header.writers
                                .StaticHeadersWriter(
                                "Content-Security-Policy",
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                "style-src 'self' 'unsafe-inline'; " +
                                "img-src 'self' data:; " +
                                "font-src 'self' data:; " +
                                "connect-src 'self' http://localhost:* ws://localhost:*; " +
                                "frame-ancestors 'none'"
                        ))
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:8081",
                "http://localhost:8080"
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
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity, UserDetailsServiceImpl userService) throws Exception {
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
            user.setRealName(username);
            user.setEmail(username + "@quizapp.com");
            user.setRole(role);
            user.setStatus("1");
            try {
                userService.registerUser(user);
            }
            catch (Exception e) {
                log.warn("Add default user fail");
            }
            log.info("Create default User success:" + username + "(role: "+ role + ")");
        }
    }
}
