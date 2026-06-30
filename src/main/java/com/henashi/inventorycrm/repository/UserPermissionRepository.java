package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {

    List<UserPermission> findByUserId(Long userId);

    Optional<UserPermission> findByUserIdAndPermission_Key(Long userId, String permissionKey);

    void deleteByUserId(Long userId);
}
