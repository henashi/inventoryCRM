package com.henashi.inventorycrm.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private boolean createDefaultUsers = false;
}
