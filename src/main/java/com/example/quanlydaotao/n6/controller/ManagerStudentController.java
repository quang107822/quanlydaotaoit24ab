package com.example.quanlydaotao.n6.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.ManagerStudentRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.ManagerStudentResponse;
import com.example.quanlydaotao.n6.service.ManagerStudentService;

@RestController
@RequestMapping("/api/v1/manager-students")
public class ManagerStudentController {

    private final ManagerStudentService service;

    public ManagerStudentController(ManagerStudentService service) {
        this.service = service;
    }

    @GetMapping
    public List<ManagerStudentResponse> getAll() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManagerStudentResponse assign(@Valid @RequestBody ManagerStudentRequest request) {
        return service.assign(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
