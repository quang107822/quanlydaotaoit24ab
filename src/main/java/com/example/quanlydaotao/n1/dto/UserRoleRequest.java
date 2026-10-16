package com.example.quanlydaotao.n1.dto;

import java.util.UUID;

/**
 * Dữ liệu gán 1 role cho 1 user (POST /api/user-roles).
 * createdBy/updatedBy là id người thao tác (tuỳ chọn, dùng cho audit).
 */
public class UserRoleRequest {

    private UUID userId;
    private UUID roleId;
    private UUID createdBy;
    private UUID updatedBy;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
