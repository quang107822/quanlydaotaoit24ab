package com.example.quanlydaotao.n3.controller;

import com.example.quanlydaotao.n3.dto.ApiResponse;
import com.example.quanlydaotao.n3.dto.EmployeePositionResponse;
import com.example.quanlydaotao.n3.entity.EmployeePosition;
import com.example.quanlydaotao.n3.service.EmployeePositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EmployeePositionController {
    @Autowired
    private EmployeePositionService service;

    @PostMapping("/employees/{employeeId}/positions/{positionId}")
    public ResponseEntity<ApiResponse<EmployeePositionResponse>> assignPosition(
            @PathVariable UUID employeeId, @PathVariable UUID positionId, @RequestBody EmployeePosition body) {
        try {
            EmployeePosition result = service.assignPosition(employeeId, positionId, body);
            EmployeePositionResponse response = new EmployeePositionResponse(
                    result.getId(), result.getEmployee().getId(), result.getPosition().getId(),
                    result.getDescription(), result.getStartDate(), result.getEndDate(), result.getIsActive());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ApiResponse<>(200, "Phân công vị trí làm việc cho giảng viên thành công", response));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ApiResponse<>(404, e.getMessage(), null));
        }
    }

    @GetMapping("/employees/{employeeId}/positions")
    public ResponseEntity<ApiResponse<List<EmployeePosition>>> getEmployeePositions(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", service.findByEmployeeId(employeeId)));
    }

    @GetMapping("/employee-positions/{id}")
    public ResponseEntity<ApiResponse<EmployeePosition>> getById(@PathVariable UUID id) {
        return service.findById(id)
                .map(val -> ResponseEntity.ok(new ApiResponse<>(200, "Success", val)))
                .orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null)));
    }

    @PutMapping("/employee-positions/{id}")
    public ResponseEntity<ApiResponse<EmployeePosition>> update(@PathVariable UUID id,
            @RequestBody EmployeePosition body) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(200, "Updated successfully", service.update(id, body)));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null));
        }
    }

    @DeleteMapping("/employees/{employeeId}/positions/{positionId}")
    public ResponseEntity<ApiResponse<Void>> unassignPosition(@PathVariable UUID employeeId,
            @PathVariable UUID positionId) {
        service.unassignPosition(employeeId, positionId);
        return ResponseEntity.ok(new ApiResponse<>(200, "Unassigned successfully", null));
    }

    @GetMapping("/positions/{positionId}/employees")
    public ResponseEntity<ApiResponse<List<EmployeePosition>>> getPositionEmployees(@PathVariable UUID positionId) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", service.findByPositionId(positionId)));
    }

    @PatchMapping("/employee-positions/{id}/end-date")
    public ResponseEntity<ApiResponse<EmployeePosition>> updateEndDate(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        String endDateStr = body.get("end_date");
        if (endDateStr == null)
            return ResponseEntity.badRequest().build();

        LocalDateTime endDate = LocalDateTime.parse(endDateStr, DateTimeFormatter.ISO_DATE_TIME);
        return service.updateEndDate(id, endDate)
                .map(val -> ResponseEntity.ok(new ApiResponse<>(200, "End date updated", val)))
                .orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null)));
    }
}
