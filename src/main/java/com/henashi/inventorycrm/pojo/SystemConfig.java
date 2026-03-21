package com.henashi.inventorycrm.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 系统配置实体（用于存储配置信息）
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_config")
@SQLRestriction(value = "is_deleted = false")
@SQLDelete(sql = "update system_config set is_deleted = true where id = ?")
public class SystemConfig extends BaseEntity {

    /**
     * 配置键
     */
    @Column(name = "config_key", unique = true, nullable = false, length = 100)
    private String configKey;

    /**
     * 配置值
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /**
     * 配置描述
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 配置分组
     */
    @Column(name = "config_group", length = 50)
    private String configGroup = "default";

}
