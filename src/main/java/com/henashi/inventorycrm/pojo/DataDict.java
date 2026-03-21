package com.henashi.inventorycrm.pojo;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "data_dict",
        indexes = {
                @Index(name = "idx_group_code", columnList = "group_code"),
                @Index(name = "idx_param_code", columnList = "param_code"),
                @Index(name = "idx_group_param_code", columnList = "group_code, param_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_group_param_code", columnNames = {"group_code", "param_code"})
        }
)
@SQLRestriction(value = "is_deleted = false")
@SQLDelete(sql = "update data_dict set is_deleted = true where id = ?")
public class DataDict extends BaseEntity {

    @Column(name = "group_code", nullable = false, length = 50)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;

    @Column(name = "param_name", nullable = false, length = 50)
    private String paramName;

    @Column(name = "param_code", nullable = false, length = 50)
    private String paramCode;

    @Column(name = "param_value", length = 50)
    private String paramValue;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private DataDictStatus status = DataDictStatus.DICT_STATUS_ACTIVE;

    @Getter
    public enum DataDictStatus {
        DICT_STATUS_ACTIVE("active"),
        DICT_STATUS_PAUSED("paused");

        public final String statusName;

        DataDictStatus(String statusName) {
            this.statusName = statusName;
        }
    }
}
