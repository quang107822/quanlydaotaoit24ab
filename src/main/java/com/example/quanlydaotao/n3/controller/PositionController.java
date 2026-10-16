package com.example.quanlydaotao.n3.controller;

import com.example.quanlydaotao.n3.dto.ApiResponse;
import com.example.quanlydaotao.n3.entity.Position;
import com.example.quanlydaotao.n3.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/positions")
public class PositionController {
    @Autowired
    private PositionService service;

    @PostMapping
    public ResponseEntity<ApiResponse<Position>> create(@RequestBody Position entity) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "Created successfully", service.save(entity)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Position>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", service.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Position>> getById(@PathVariable UUID id) {
        return service.findById(id)
            .map(val -> ResponseEntity.ok(new ApiResponse<>(200, "Success", val)))
            .orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Position>> update(@PathVariable UUID id, @RequestBody Position entity) {
        return service.findById(id).map(existing -> {
            if (entity.getCode() != null) existing.setCode(entity.getCode());
            if (entity.getTitle() != null) existing.setTitle(entity.getTitle());
            if (entity.getLevel() != null) existing.setLevel(entity.getLevel());
            if (entity.getDepartment() != null) existing.setDepartment(entity.getDepartment());
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
    public ResponseEntity<ApiResponse<List<Position>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", service.search(keyword)));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<Position>>> getByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", service.findByDepartmentId(departmentId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Position>> updateStatus(@PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("is_active");
        if (isActive == null) return ResponseEntity.badRequest().build();
        return service.updateStatus(id, isActive)
            .map(val -> ResponseEntity.ok(new ApiResponse<>(200, "Status updated", val)))
            .orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null)));
    }
}
