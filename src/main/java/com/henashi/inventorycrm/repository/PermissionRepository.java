package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByKey(String key);

    List<Permission> findByModule(String module);

    boolean existsByKey(String key);
}
