package com.henashi.inventorycrm.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.henashi.inventorycrm.config.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final CacheProperties cacheProperties;

    @Bean("shortCache")
    public CacheManager shortCache() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        // 全局默认配置（所有未单独注册的缓存使用此策略）
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(cacheProperties.getInitialCapacity()) // 初始容量
                .maximumSize(cacheProperties.getMaxSize()) // 最大条目数
                .expireAfterWrite(cacheProperties.getExpireWriteMinutes(), TimeUnit.MINUTES) // 写入后过期
                .expireAfterAccess(cacheProperties.getExpireAccessMinutes(), TimeUnit.MINUTES) // 访问后过期
                .recordStats() // 启用统计，必开
        );
        // 静态模式：仅管理指定缓存名（动态模式默认，按需创建）
        // cacheManager.setCacheNames(Arrays.asList("userCache", "productCache"));
        return cacheManager;
    }
}
