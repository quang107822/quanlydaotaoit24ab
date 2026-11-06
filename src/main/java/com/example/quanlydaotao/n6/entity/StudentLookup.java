package com.example.quanlydaotao.n6.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "students")
@Data
public class StudentLookup {

    @Id
    private UUID id;

    @Column(name = "student_code")
    private String code;

    @Column(name = "full_name")
    @Nationalized
    private String fullName;

}
