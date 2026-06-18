package com.henashi.inventorycrm.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret = "";
    private long expiration = 3600000;
    private long refreshExpiration = 86400000;
}
