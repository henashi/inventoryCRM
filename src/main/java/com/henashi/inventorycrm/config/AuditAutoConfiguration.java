package com.henashi.inventorycrm.config;

import com.henashi.inventorycrm.aspect.OperationLogAspect;
import com.henashi.inventorycrm.aspect.detector.SafeFieldDetector;
import com.henashi.inventorycrm.service.OperationLogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class AuditAutoConfiguration {

    @Bean
    public SafeFieldDetector safeFieldDetector() {
        return new SafeFieldDetector();
    }

    @Bean
    public OperationLogAspect operationLogAspect(OperationLogService operationLogService) {
        return new OperationLogAspect(operationLogService);
    }

}
