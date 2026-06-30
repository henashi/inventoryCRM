package com.henashi.inventorycrm.mapper;

import com.henashi.inventorycrm.dto.UserCreateDTO;
import com.henashi.inventorycrm.dto.UserDTO;
import com.henashi.inventorycrm.pojo.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING
)
public interface UserMapper {

    default User toEntity(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        User user = new User();
        user.setId(userDTO.id());
        user.setUsername(userDTO.username());
        user.setRealName(userDTO.realName());
        user.setEmail(userDTO.email());
        // role 由 Service 层通过 RoleRepository 设置
        user.setStatus(toStatusString(userDTO.status()));
        user.setLastLoginAt(userDTO.lastLoginAt());
        user.setRemark(userDTO.remark());
        return user;
    }

    default User createToEntity(UserCreateDTO userCreateDTO) {
        if (userCreateDTO == null) {
            return null;
        }

        User user = new User();
        user.setUsername(userCreateDTO.username());
        user.setPassword(userCreateDTO.password());
        // role 由 Service 层通过 RoleRepository 设置
        user.setRemark(userCreateDTO.remark());
        return user;
    }

    default UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }

        return new UserDTO(
                user.getId(),
                user.getUsername(),
                resolveRealName(user),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName() : null,
                toStatusInteger(user.getStatus()),
                user.getLastLoginAt(),
                user.getCreatedTime(),
                user.getRemark()
        );
    }

    default User partialUpdate(UserDTO userDTO, @MappingTarget User user) {
        if (userDTO == null || user == null) {
            return user;
        }

        if (userDTO.username() != null) {
            user.setUsername(userDTO.username());
        }
        if (userDTO.realName() != null) {
            user.setRealName(userDTO.realName());
        }
        if (userDTO.email() != null) {
            user.setEmail(userDTO.email());
        }
        // role 由 Service 层通过 RoleRepository 设置
        if (userDTO.role() != null) {
        }
        if (userDTO.status() != null) {
            user.setStatus(toStatusString(userDTO.status()));
        }
        if (userDTO.lastLoginAt() != null) {
            user.setLastLoginAt(userDTO.lastLoginAt());
        }
        if (userDTO.remark() != null) {
            user.setRemark(userDTO.remark());
        }
        return user;
    }

    private Integer toStatusInteger(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return Integer.valueOf(status);
    }

    private String toStatusString(Integer status) {
        if (status == null) {
            return null;
        }
        return String.valueOf(status);
    }

    private String resolveRealName(User user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        return user.getUsername();
    }
}
