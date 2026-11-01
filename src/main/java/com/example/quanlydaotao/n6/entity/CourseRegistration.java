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
@Table(name = "course_registrations")
@Data
public class CourseRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "registration_period_id")
    private UUID registrationPeriodId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_class_id")
    private UUID courseClassId;

    @Column(name = "ecourse_section_id")
    private UUID ecourseSectionId;

    @Column(name = "registration_type")
    private Integer registrationType;

    @Column(name = "replaced_grade_id")
    private UUID replacedGradeId;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    void onCreate() {
        registeredAt = registeredAt == null ? LocalDateTime.now() : registeredAt;
        createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        isPaid = isPaid == null ? false : isPaid;
        status = status == null ? 1 : status;
    }
}
