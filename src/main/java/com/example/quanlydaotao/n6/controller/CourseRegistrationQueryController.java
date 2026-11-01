package com.example.quanlydaotao.n6.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.CourseRegistrationResponse;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.PaymentStatusRequest;
import com.example.quanlydaotao.n6.service.CourseRegistrationQueryService;

@RestController
@RequestMapping("/api/v1/course-registrations")
public class CourseRegistrationQueryController {

    private final CourseRegistrationQueryService service;

    public CourseRegistrationQueryController(CourseRegistrationQueryService service) {
        this.service = service;
    }

    @GetMapping("/section/{sectionId}")
    public List<CourseRegistrationResponse> listBySection(@PathVariable UUID sectionId) {
        return service.listBySection(sectionId);
    }

    @PatchMapping("/{id}/payment-status")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePayment(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentStatusRequest request) {
        service.updatePayment(id, request);
    }
}
