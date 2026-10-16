package com.example.quanlydaotao.n1.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bảng trung gian gán vai trò (role) cho người dùng - n-quyền cho n-user.
 *
 * Mapping khớp bảng [dbo].[user_roles] trong database/it24ab.sql:
 * id, user_id, role_id, created_at, updated_at, created_by, updated_by,
 * deleted_at, deleted_by, is_active.
 *
 * Bảng có FK tới users(id) và roles(id): FK_user_roles_user, FK_user_roles_role.
 * Không có ràng buộc unique (user_id, role_id) nên tầng service phải tự kiểm tra trùng.
 */
@Entity
@Table(name = "user_roles")
public class UserRole {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID userId;

    @Column(name = "role_id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID roleId;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", columnDefinition = "uniqueidentifier")
    private UUID createdBy;

    @Column(name = "updated_by", columnDefinition = "uniqueidentifier")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", columnDefinition = "uniqueidentifier")
    private UUID deletedBy;

    // ==================== Getter + Setter ====================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(UUID deletedBy) {
        this.deletedBy = deletedBy;
    }
}
