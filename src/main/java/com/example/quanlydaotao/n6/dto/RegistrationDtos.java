package com.example.quanlydaotao.n6.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class RegistrationDtos {

    private RegistrationDtos() {
    }

    public record PageResult<T>(long total, int page, int size, java.util.List<T> items) {
    }

    public record RegistrationPeriodRequest(
            @NotBlank @Size(max = 200) String name,
            @NotNull UUID semesterId,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime,
            @Min(0) @Max(255) Integer minCredits,
            @Min(1) @Max(255) Integer maxCredits,
            Boolean allowRetake,
            Boolean isOpen) {
    }

    public record RegistrationPeriodResponse(
            UUID id,
            String name,
            UUID semesterId,
            String semesterName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer minCredits,
            Integer maxCredits,
            Boolean allowRetake,
            Boolean isOpen,
            Boolean isActive) {
    }

    public record OpenRequest(@NotNull Boolean isOpen) {
    }

    public record EcourseSectionRequest(
            @NotNull UUID courseId,
            @NotNull UUID semesterId,
            @NotBlank @Size(max = 50) String code,
            @NotNull @Min(1) Integer maxCapacity,
            @Min(1) @Max(3) Integer status) {
    }

    public record EcourseSectionResponse(
            UUID id,
            UUID courseId,
            UUID semesterId,
            String semesterName,
            String code,
            String courseCode,
            String courseName,
            Integer credits,
            Integer maxCapacity,
            Integer currentEnrolled,
            Integer status,
            Long rowVersion) {
    }

    public record SectionStatusRequest(@NotNull @Min(1) @Max(3) Integer status) {
    }

    public record EquivalentCourseRequest(
            @NotNull UUID originalCourseId,
            @NotNull UUID equivalentCourseId,
            @NotNull @Min(1) @Max(2) Integer equivalenceType,
            java.time.LocalDate effectDate,
            @Size(max = 500) String note) {
    }

    public record EquivalentCourseResponse(
            UUID id,
            UUID originalCourseId,
            String originalCourseCode,
            String originalCourseName,
            UUID equivalentCourseId,
            String equivalentCourseCode,
            String equivalentCourseName,
            Integer equivalenceType,
            java.time.LocalDate effectDate,
            String note) {
    }

    public record ListStudentRequest(
            @NotNull UUID studentId,
            @Min(0) @Max(255) Integer maxAllowedCredits) {
    }

    public record ListStudentResponse(
            UUID id,
            UUID registrationPeriodId,
            UUID studentId,
            String studentCode,
            String studentName,
            Integer maxAllowedCredits,
            Boolean isActive) {
    }

    public record ImportResult(int added, int skipped, java.util.List<String> errors) {
    }

    public record DepartmentStatistics(
            UUID departmentId,
            String departmentName,
            long registrationCount,
            long retakeCount,
            long improvementCount) {
    }

    public record ManagerStudentRequest(
            @NotNull UUID studentId,
            @NotNull UUID managerId,
            @NotNull @Min(1) @Max(2) Integer roleType,
            Boolean isPrimary) {
    }

    public record ManagerStudentResponse(
            UUID id,
            UUID studentId,
            String studentCode,
            String studentName,
            UUID managerId,
            String managerCode,
            String managerName,
            Integer roleType,
            Boolean isPrimary) {
    }

    public record CourseRegistrationResponse(
            UUID id,
            UUID registrationPeriodId,
            UUID studentId,
            String studentCode,
            String studentName,
            UUID ecourseSectionId,
            String sectionCode,
            String courseCode,
            String courseName,
            Integer registrationType,
            UUID replacedGradeId,
            Boolean isPaid,
            Integer status,
            LocalDateTime registeredAt) {
    }

    public record PaymentStatusRequest(@NotNull Boolean isPaid) {
    }

    public record RegistrationStatistics(
            UUID departmentId,
            String departmentName,
            long registrationCount,
            long retakeCount,
            long improvementCount) {
    }

    public record PeriodSummary(
            UUID periodId,
            String periodName,
            long eligibleStudents,
            long registeredStudents,
            long registrationCount,
            long paidCount,
            long activeSections,
            long availableSeats) {
    }

    public record CourseOption(UUID id, String code, String name, BigDecimal credits) {
    }

    public record SemesterOption(UUID id, String code, String name) {
    }

    public record PersonOption(UUID id, String code, String fullName) {
    }
}
