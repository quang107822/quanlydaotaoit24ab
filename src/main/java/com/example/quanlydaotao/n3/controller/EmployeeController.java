package com.example.quanlydaotao.n3.controller;

import com.example.quanlydaotao.n3.dto.ApiResponse;
import com.example.quanlydaotao.n3.dto.EmployeeRequest;
import com.example.quanlydaotao.n3.dto.EmployeeResponse;
import com.example.quanlydaotao.n3.entity.Department;
import com.example.quanlydaotao.n3.entity.Employee;
import com.example.quanlydaotao.n3.entity.EmployeePosition;
import com.example.quanlydaotao.n3.entity.Position;
import com.example.quanlydaotao.n3.service.DepartmentService;
import com.example.quanlydaotao.n3.service.EmployeePositionService;
import com.example.quanlydaotao.n3.service.EmployeeService;
import com.example.quanlydaotao.n3.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EmployeeController {
    @Autowired
    private EmployeeService service;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private PositionService positionService;
    @Autowired
    private EmployeePositionService employeePositionService;

    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@RequestBody EmployeeRequest request) {
        Employee emp = new Employee();
        emp.setUserId(request.getUserId());
        emp.setCode(request.getCode());
        emp.setFullName(request.getFullName());
        emp.setDateOfBirth(request.getDateOfBirth());
        emp.setGender(request.getGender() != null ? request.getGender() : "Nam");
        emp.setEmail(request.getEmail());
        emp.setPhone(request.getPhone());
        emp.setAddress(request.getAddress());
        emp.setHireDate(request.getHireDate());
        emp.setContractType(request.getContractType());
        emp.setSalaryCoefficient(request.getSalaryCoefficient());
        emp.setAcademicDegree(request.getAcademicDegree());
        emp.setAcademicTitle(request.getAcademicTitle());
        emp.setSpecialization(request.getSpecialization());

        Department dept = departmentService.findById(request.getDepartmentId())
            .orElseThrow(() -> new RuntimeException("Department not found"));
        emp.setDepartment(dept);

        Position pos = null;
        if (request.getPositionId() != null) {
            pos = positionService.findById(request.getPositionId()).orElse(null);
            emp.setPosition(pos);
        }

        Employee saved = service.save(emp);
        
        // Also create initial EmployeePosition record if position assigned
        if (pos != null) {
            EmployeePosition ep = new EmployeePosition();
            ep.setEmployee(saved);
            ep.setPosition(pos);
            ep.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now());
            ep.setDescription("Bổ nhiệm chức vụ ban đầu");
            employeePositionService.assignPosition(saved.getId(), pos.getId(), ep);
        }

        EmployeeResponse response = new EmployeeResponse(
            saved.getId(), saved.getCode(), saved.getFullName(), saved.getEmail(), saved.getIsActive(), saved.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(201, "Thêm mới hồ sơ giảng viên/nhân viên thành công", response));
    }

    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<Employee>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", service.findAll()));
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<Employee>> getById(@PathVariable UUID id) {
        return service.findById(id)
            .map(val -> ResponseEntity.ok(new ApiResponse<>(200, "Success", val)))
            .orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null)));
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<Employee>> update(@PathVariable UUID id, @RequestBody Employee entity) {
        return service.findById(id).map(existing -> {
            if (entity.getCode() != null) existing.setCode(entity.getCode());
            if (entity.getFullName() != null) existing.setFullName(entity.getFullName());
            if (entity.getGender() != null) existing.setGender(entity.getGender());
            if (entity.getDateOfBirth() != null) existing.setDateOfBirth(entity.getDateOfBirth());
            if (entity.getEmail() != null) existing.setEmail(entity.getEmail());
            if (entity.getPhone() != null) existing.setPhone(entity.getPhone());
            if (entity.getAddress() != null) existing.setAddress(entity.getAddress());
            if (entity.getHireDate() != null) existing.setHireDate(entity.getHireDate());
            if (entity.getContractType() != null) existing.setContractType(entity.getContractType());
            if (entity.getSalaryCoefficient() != null) existing.setSalaryCoefficient(entity.getSalaryCoefficient());
            if (entity.getAcademicDegree() != null) existing.setAcademicDegree(entity.getAcademicDegree());
            if (entity.getAcademicTitle() != null) existing.setAcademicTitle(entity.getAcademicTitle());
            if (entity.getSpecialization() != null) existing.setSpecialization(entity.getSpecialization());
            if (entity.getDepartment() != null && entity.getDepartment().getId() != null) {
                departmentService.findById(entity.getDepartment().getId()).ifPresent(existing::setDepartment);
            }
            if (entity.getPosition() != null && entity.getPosition().getId() != null) {
                positionService.findById(entity.getPosition().getId()).ifPresent(existing::setPosition);
            }
            if (entity.getIsActive() != null) existing.setIsActive(entity.getIsActive());
            return ResponseEntity.ok(new ApiResponse<>(200, "Updated successfully", service.save(existing)));
        }).orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null)));
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        return service.findById(id).map(existing -> {
            service.deleteById(id);
            return ResponseEntity.ok(new ApiResponse<Void>(200, "Deleted successfully", null));
        }).orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<Void>(404, "Not Found", null)));
    }

    @GetMapping("/employees/search")
    public ResponseEntity<ApiResponse<List<Employee>>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", service.search(keyword)));
    }

    @GetMapping("/employees/department/{departmentId}")
    public ResponseEntity<ApiResponse<List<Employee>>> getByDepartment(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(new ApiResponse<>(200, "Success", service.findByDepartmentId(departmentId)));
    }

    @PatchMapping("/employees/{id}/status")
    public ResponseEntity<ApiResponse<Employee>> updateStatus(@PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("is_active");
        if (isActive == null) return ResponseEntity.badRequest().build();
        return service.updateStatus(id, isActive)
            .map(val -> ResponseEntity.ok(new ApiResponse<>(200, "Status updated", val)))
            .orElseGet(() -> ResponseEntity.status(404).body(new ApiResponse<>(404, "Not Found", null)));
    }
}
