package com.example.quanlydaotao.n6.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.EquivalentCourseRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.EquivalentCourseResponse;
import com.example.quanlydaotao.n6.service.EquivalentCourseService;

@RestController
@RequestMapping("/api/v1/equivalent-courses")
public class EquivalentCourseController {

    private final EquivalentCourseService service;

    public EquivalentCourseController(EquivalentCourseService service) {
        this.service = service;
    }

    @GetMapping
    public List<EquivalentCourseResponse> getAll() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquivalentCourseResponse create(@Valid @RequestBody EquivalentCourseRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EquivalentCourseResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody EquivalentCourseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
