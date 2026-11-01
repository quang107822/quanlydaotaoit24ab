package com.example.quanlydaotao.n6.dto;

import lombok.Data;

@Data
public class EcourseSectionDTO {
    private Long id;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private Integer maxCapacity;
    private Integer currentEnrolled;
    private Integer status;
}