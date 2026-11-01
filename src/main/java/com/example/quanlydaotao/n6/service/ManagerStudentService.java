package com.example.quanlydaotao.n6.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.ManagerStudentRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.ManagerStudentResponse;
import com.example.quanlydaotao.n6.entity.EmployeeLookup;
import com.example.quanlydaotao.n6.entity.ManagerStudent;
import com.example.quanlydaotao.n6.entity.StudentLookup;
import com.example.quanlydaotao.n6.repository.EmployeeLookupRepository;
import com.example.quanlydaotao.n6.repository.ManagerStudentRepository;
import com.example.quanlydaotao.n6.repository.StudentLookupRepository;

@Service
public class ManagerStudentService {

    private final ManagerStudentRepository managerRepository;
    private final StudentLookupRepository studentRepository;
    private final EmployeeLookupRepository employeeRepository;

    public ManagerStudentService(
            ManagerStudentRepository managerRepository,
            StudentLookupRepository studentRepository,
            EmployeeLookupRepository employeeRepository) {
        this.managerRepository = managerRepository;
        this.studentRepository = studentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<ManagerStudentResponse> list() {
        return managerRepository.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ManagerStudentResponse assign(ManagerStudentRequest request) {
        StudentLookup student = findStudent(request.studentId());
        EmployeeLookup manager = findManager(request.managerId());
        if (Boolean.TRUE.equals(request.isPrimary()) && request.roleType() == 1) {
            managerRepository.findByStudentIdAndRoleTypeAndIsPrimaryTrueAndDeletedAtIsNull(
                            student.getId(), 1)
                    .forEach(existing -> {
                        existing.setIsPrimary(false);
                        managerRepository.save(existing);
                    });
        }

        ManagerStudent assignment = new ManagerStudent();
        assignment.setStudentId(student.getId());
        assignment.setManagerId(manager.getId());
        assignment.setRoleType(request.roleType());
        assignment.setIsPrimary(request.isPrimary() != null && request.isPrimary());
        return toResponse(managerRepository.save(assignment));
    }

    @Transactional
    public void delete(UUID id) {
        ManagerStudent assignment = managerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phân công."));
        assignment.setDeletedAt(LocalDateTime.now());
        managerRepository.save(assignment);
    }

    private ManagerStudentResponse toResponse(ManagerStudent assignment) {
        StudentLookup student = findStudent(assignment.getStudentId());
        EmployeeLookup manager = findManager(assignment.getManagerId());
        return new ManagerStudentResponse(
                assignment.getId(),
                student.getId(),
                student.getCode(),
                student.getFullName(),
                manager.getId(),
                manager.getCode(),
                manager.getFullName(),
                assignment.getRoleType(),
                assignment.getIsPrimary());
    }

    private StudentLookup findStudent(UUID id) {
        return studentRepository.findFirstById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sinh viên."));
    }

    private EmployeeLookup findManager(UUID id) {
        return employeeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cán bộ."));
    }
}
