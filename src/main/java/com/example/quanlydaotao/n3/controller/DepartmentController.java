package com.example.quanlydaotao.n3.controller;

import com.example.quanlydaotao.n3.dto.ApiResponse;
import com.example.quanlydaotao.n3.entity.Department;
import com.example.quanlydaotao.n3.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {
    @Autowired
    private DepartmentService service;

    @PostMapping
    public ResponseEntity<ApiResponse<Department>> create(@RequestBody Department entity) {
        Department saved = service.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "Created successfully", saved));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Department>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", service.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Department>> getById(@PathVariable UUID id) {
        return service.findById(id)
            .map(val -> ResponseEntity.ok(new ApiResponse<>(200, "Success", val)))
            .orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Department>> update(@PathVariable UUID id, @RequestBody Department entity) {
        return service.findById(id).map(existing -> {
            if (entity.getCode() != null) existing.setCode(entity.getCode());
            if (entity.getName() != null) existing.setName(entity.getName());
            if (entity.getPhone() != null) existing.setPhone(entity.getPhone());
            if (entity.getEmail() != null) existing.setEmail(entity.getEmail());
            if (entity.getEstablishedDate() != null) existing.setEstablishedDate(entity.getEstablishedDate());
            if (entity.getIsActive() != null) existing.setIsActive(entity.getIsActive());
            return ResponseEntity.ok(new ApiResponse<>(200, "Updated successfully", service.save(existing)));
        }).orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        return service.findById(id).map(existing -> {
            service.deleteById(id);
            return ResponseEntity.ok(new ApiResponse<Void>(200, "Deleted successfully", null));
        }).orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<Void>(404, "Not Found", null)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Department>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", service.search(keyword)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Department>> updateStatus(@PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("is_active");
        if (isActive == null) return ResponseEntity.badRequest().build();
        return service.updateStatus(id, isActive)
            .map(val -> ResponseEntity.ok(new ApiResponse<>(200, "Status updated", val)))
            .orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null)));
    }
}
