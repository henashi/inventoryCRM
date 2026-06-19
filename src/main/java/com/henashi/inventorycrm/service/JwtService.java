package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.config.properties.JwtProperties;
import com.henashi.inventorycrm.pojo.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
@Slf4j
@RefreshScope
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = buildClaims("ACCESS", 0);
        return createToken(claims, userDetails.getUsername(), jwtProperties.getExpiration());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = buildClaims("REFRESH", 0);
        return createToken(claims, userDetails.getUsername(), jwtProperties.getRefreshExpiration());
    }

    public String generateToken(User user) {
        Map<String, Object> claims = buildClaims("ACCESS", getCurrentTokenVersion(user));
        return createToken(claims, user.getUsername(), jwtProperties.getExpiration());
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = buildClaims("REFRESH", getCurrentTokenVersion(user));
        return createToken(claims, user.getUsername(), jwtProperties.getRefreshExpiration());
    }

    public String generateToken(String username) {
        Map<String, Object> claims = buildClaims("ACCESS", 0);
        return createToken(claims, username, jwtProperties.getExpiration());
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = buildClaims("REFRESH", 0);
        return createToken(claims, username, jwtProperties.getRefreshExpiration());
    }

    private Map<String, Object> buildClaims(String type, int tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", type);
        claims.put("tokenVersion", tokenVersion);
        return claims;
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean validateAccessToken(String token, User user) {
        return validateTokenForType(token, user, "ACCESS");
    }

    public boolean validateRefreshToken(String token, User user) {
        return validateTokenForType(token, user, "REFRESH");
    }

    private boolean validateTokenForType(String token, User user, String expectedType) {
        if (!validateToken(token)) {
            return false;
        }
        if (!Objects.equals(expectedType, getTokenType(token))) {
            return false;
        }
        if (!Objects.equals(extractUsername(token), user.getUsername())) {
            return false;
        }
        return extractTokenVersionOrDefault(token) == getCurrentTokenVersion(user);
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Long getExpirationTime() {
        return jwtProperties.getExpiration();
    }

    public Long getRefreshExpirationTime() {
        return jwtProperties.getRefreshExpiration();
    }

    public String getTokenType(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("type", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Integer getTokenVersion(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("tokenVersion", Integer.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isRefreshToken(String token) {
        String type = getTokenType(token);
        return "REFRESH".equals(type);
    }

    public boolean isAccessToken(String token) {
        String type = getTokenType(token);
        return "ACCESS".equals(type);
    }

    public Date getTokenIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    private int extractTokenVersionOrDefault(String token) {
        Integer tokenVersion = getTokenVersion(token);
        return tokenVersion == null ? 0 : tokenVersion;
    }

    private int getCurrentTokenVersion(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }
}
