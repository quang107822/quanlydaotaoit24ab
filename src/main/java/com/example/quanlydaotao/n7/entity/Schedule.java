package com.example.quanlydaotao.n7.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "schedules")
@Getter
@Setter
public class Schedule {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "course_section_id")
    private UUID courseSectionId;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    // Giữ nguyên kiểu uniqueidentifier trong SQL Server
    @Column(name = "date")
    private UUID date;

    @Column(name = "shift_id")
    private UUID shiftId;

    @Column(name = "start_period")
    private Integer startPeriod;

    @Column(name = "end_period")
    private Integer endPeriod;

    @Column(name = "number_of_periods")
    private Integer numberOfPeriods;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "mode", length = 100)
    private String mode;

    @Column(name = "status", length = 255)
    private String status;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "schedule_status", length = 50)
    private String scheduleStatus;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "is_active")
    private Boolean isActive;
}