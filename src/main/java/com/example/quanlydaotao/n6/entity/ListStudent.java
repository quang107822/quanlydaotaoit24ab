package com.example.quanlydaotao.n6.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "list_students")
@Data
public class ListStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "registration_period_id", nullable = false)
    private UUID registrationPeriodId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "max_allowed_credits")
    private Integer maxAllowedCredits;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    void onCreate() {
        createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        isActive = isActive == null ? true : isActive;
    }
}
