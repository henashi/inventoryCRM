package com.henashi.inventorycrm.pojo;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
@SuperBuilder
public class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "status_updated_time")
    private LocalDateTime statusUpdatedTime;

    @Column(name = "content_updated_time")
    private LocalDateTime contentUpdatedTime;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "content_updated_by")
    private String contentUpdatedBy;

    @Column(name = "status_updated_by")
    private String statusUpdatedBy;

    @Column(name = "status")
    private String status;

    @Column(name = "deleted", nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;
}
