package com.example.quanlydaotao.n6.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "employees")
@Data
public class EmployeeLookup {

    @Id
    private UUID id;

    @Column(name = "code")
    private String code;

    @Column(name = "full_name")
    @Nationalized
    private String fullName;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
