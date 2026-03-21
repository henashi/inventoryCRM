package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.SystemConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    Optional<SystemConfig> findByConfigKey(String configKey);

    List<SystemConfig> findByConfigGroup(String configGroup);

    boolean existsByConfigKey(@NotBlank(message = "配置键不能为空") @Size(max = 100, message = "配置键不能超过100字符") @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "配置键只能包含字母、数字、点和下划线") String s);
}
