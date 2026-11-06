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
@Table(name = "departments")
@Data
public class DepartmentLookup {

    @Id
    private UUID id;

    @Column(name = "name")
    @Nationalized
    private String name;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
