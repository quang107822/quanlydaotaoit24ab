package com.example.quanlydaotao.n6.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.EcourseSectionRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.EcourseSectionResponse;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.PageResult;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.SectionStatusRequest;
import com.example.quanlydaotao.n6.service.EcourseSectionService;

@RestController
@RequestMapping("/api/v1/ecourse-sections")
@Validated
public class EcourseSectionController {

    private final EcourseSectionService service;

    public EcourseSectionController(EcourseSectionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResult<EcourseSectionResponse> getAll(
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) @Min(1) @Max(3) Integer status,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(semesterId, status, courseId, keyword, page, size);
    }

    @GetMapping("/{id}")
    public EcourseSectionResponse getById(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EcourseSectionResponse create(@Valid @RequestBody EcourseSectionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EcourseSectionResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody EcourseSectionRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public EcourseSectionResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SectionStatusRequest request) {
        return service.changeStatus(id, request.status());
    }
}
