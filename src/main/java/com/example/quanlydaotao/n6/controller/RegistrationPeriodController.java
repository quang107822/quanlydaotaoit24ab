package com.example.quanlydaotao.n6.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import com.example.quanlydaotao.n6.dto.RegistrationDtos.OpenRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.PageResult;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.RegistrationPeriodRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.RegistrationPeriodResponse;
import com.example.quanlydaotao.n6.service.RegistrationPeriodService;

@RestController
@RequestMapping("/api/v1/registration-periods")
@Validated
public class RegistrationPeriodController {

    private final RegistrationPeriodService service;

    public RegistrationPeriodController(RegistrationPeriodService service) {
        this.service = service;
    }

    @GetMapping
    public PageResult<RegistrationPeriodResponse> getAll(
            @RequestParam(required = false) UUID semesterId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isOpen,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(semesterId, isActive, isOpen, keyword, page, size);
    }

    @GetMapping("/{id}")
    public RegistrationPeriodResponse getById(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationPeriodResponse create(@Valid @RequestBody RegistrationPeriodRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public RegistrationPeriodResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody RegistrationPeriodRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/toggle-open")
    public RegistrationPeriodResponse toggleOpen(
            @PathVariable UUID id,
            @Valid @RequestBody OpenRequest request) {
        return service.toggle(id, request.isOpen());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
