package com.henashi.inventorycrm.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {
    private int initialCapacity = 100;
    private int maxSize = 2000;
    private int expireWriteMinutes = 5;
    private int expireAccessMinutes = 5;
}
