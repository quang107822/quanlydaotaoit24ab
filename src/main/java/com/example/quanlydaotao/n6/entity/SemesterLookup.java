package com.example.quanlydaotao.n6.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "semesters")
@Data
public class SemesterLookup {

    @Id
    private UUID id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
