package com.example.quanlydaotao.n1.dto;

import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu gán nhiều role cho 1 user:
 * - POST /api/user-roles/user/{userId} : thêm các role trong danh sách (giữ role cũ)
 * - PUT  /api/user-roles/user/{userId} : đồng bộ - danh sách gửi lên là toàn bộ role của user
 */
public class AssignRolesRequest {

    private List<UUID> roleIds;
    private UUID createdBy;
    private UUID updatedBy;

    public List<UUID> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<UUID> roleIds) {
        this.roleIds = roleIds;
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
