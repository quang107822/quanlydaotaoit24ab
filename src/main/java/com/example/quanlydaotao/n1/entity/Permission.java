package com.example.quanlydaotao.n1.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quyền chức năng chi tiết của hệ thống.
 *
 * Mapping khớp với bảng [dbo].[permissions] trong database/it24ab.sql:
 * id, code, name, module, description, created_at, updated_at, created_by,
 * updated_by, deleted_at, deleted_by, is_active.
 *
 * LƯU Ý: bảng permissions KHÔNG có cột "action" (lỗi cũ của entity đã bị loại bỏ),
 * và cột unique thật của bảng là "code" (ràng buộc UQ_permissions_code).
 */
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID id;

    // Mã quyền (vd: USER_CREATE, STUDENT_VIEW, GRADE_INPUT) - DB có UQ_permissions_code
    @Column(name = "code", nullable = false, length = 100, unique = true)
    private String code;

    // Tên hiển thị của quyền (vd: "Xem người dùng"). Không unique trong DB.
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    // Nhóm chức năng (vd: USERS, STUDENTS, GRADES, COURSES, ...)
    @Column(name = "module", length = 100)
    private String module;

    // Mô tả chi tiết quyền đó làm gì
    @Column(name = "description", length = 255)
    private String description;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
