package com.henashi.inventorycrm.repository;

import com.henashi.inventorycrm.pojo.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRole_Name(String roleName);

    List<RolePermission> findByRole_NameAndEnabled(String roleName, boolean enabled);

    Optional<RolePermission> findByRole_NameAndPermission_Key(String roleName, String permissionKey);

    void deleteByRole_Name(String roleName);
}
