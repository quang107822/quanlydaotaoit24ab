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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.ImportResult;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.ListStudentRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.ListStudentResponse;
import com.example.quanlydaotao.n6.service.ListStudentService;

@RestController
@RequestMapping("/api/v1/registration-periods/{periodId}/students")
public class ListStudentController {

    private final ListStudentService service;

    public ListStudentController(ListStudentService service) {
        this.service = service;
    }

    @GetMapping
    public List<ListStudentResponse> getAll(@PathVariable UUID periodId) {
        return service.list(periodId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListStudentResponse add(
            @PathVariable UUID periodId,
            @Valid @RequestBody ListStudentRequest request) {
        return service.add(periodId, request);
    }

    @PostMapping("/import")
    public ImportResult importExcel(
            @PathVariable UUID periodId,
            @RequestParam("file") MultipartFile file) {
        return service.importExcel(periodId, file);
    }

    @DeleteMapping("/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID periodId, @PathVariable UUID studentId) {
        service.revoke(periodId, studentId);
    }
}
