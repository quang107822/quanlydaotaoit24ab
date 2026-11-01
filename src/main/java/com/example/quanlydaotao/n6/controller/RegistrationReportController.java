package com.example.quanlydaotao.n6.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.DepartmentStatistics;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.PeriodSummary;
import com.example.quanlydaotao.n6.service.RegistrationReportService;

@RestController
@RequestMapping("/api/v1/reports")
public class RegistrationReportController {

    private final RegistrationReportService service;

    public RegistrationReportController(RegistrationReportService service) {
        this.service = service;
    }

    @GetMapping("/registrations/export")
    public ResponseEntity<ByteArrayResource> export(
            @RequestParam(required = false) UUID periodId,
            @RequestParam(required = false) UUID sectionId) {
        byte[] workbook = service.export(periodId, sectionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=registrations.xlsx")
                .contentLength(workbook.length)
                .body(new ByteArrayResource(workbook));
    }

    @GetMapping("/retake-statistics")
    public List<DepartmentStatistics> statistics(
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) UUID periodId) {
        return service.statistics(semesterId, periodId);
    }

    @GetMapping("/period-summary/{periodId}")
    public PeriodSummary summary(@PathVariable UUID periodId) {
        return service.summary(periodId);
    }
}
