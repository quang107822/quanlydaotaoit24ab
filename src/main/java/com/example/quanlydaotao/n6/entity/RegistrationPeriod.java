package com.example.quanlydaotao.n6.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "registration_periods")
@Data
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "semester_id", nullable = false)
    private UUID semesterId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "min_credits")
    private Integer minCredits = 12;

    @Column(name = "max_credits")
    private Integer maxCredits = 25;

    @Column(name = "allow_retake", nullable = false)
    private Boolean allowRetake = true;

    @Column(name = "is_open", nullable = false)
    private Boolean isOpen = true;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (minCredits == null) {
            minCredits = 12;
        }
        if (maxCredits == null) {
            maxCredits = 25;
        }
        if (allowRetake == null) {
            allowRetake = true;
        }
        if (isOpen == null) {
            isOpen = true;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
