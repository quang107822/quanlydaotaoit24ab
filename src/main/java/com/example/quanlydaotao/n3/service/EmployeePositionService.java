package com.example.quanlydaotao.n3.service;

import com.example.quanlydaotao.n3.entity.Employee;
import com.example.quanlydaotao.n3.entity.EmployeePosition;
import com.example.quanlydaotao.n3.entity.Position;
import com.example.quanlydaotao.n3.repository.EmployeePositionRepository;
import com.example.quanlydaotao.n3.repository.EmployeeRepository;
import com.example.quanlydaotao.n3.repository.PositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmployeePositionService {
    @Autowired
    private EmployeePositionRepository repository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private PositionRepository positionRepository;

    public EmployeePosition assignPosition(UUID employeeId, UUID positionId, EmployeePosition data) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow(() -> new RuntimeException("Employee not found"));
        Position position = positionRepository.findById(positionId).orElseThrow(() -> new RuntimeException("Position not found"));
        
        data.setEmployee(employee);
        data.setPosition(position);
        if (data.getStartDate() == null) {
            data.setStartDate(LocalDateTime.now());
        }
        return repository.save(data);
    }
    
    public List<EmployeePosition> findByEmployeeId(UUID employeeId) {
        return repository.findByEmployeeId(employeeId);
    }
    
    public List<EmployeePosition> findByPositionId(UUID positionId) {
        return repository.findByPositionId(positionId);
    }
    
    public Optional<EmployeePosition> findById(UUID id) {
        return repository.findById(id);
    }
    
    public EmployeePosition update(UUID id, EmployeePosition data) {
        return repository.findById(id).map(ep -> {
            ep.setStartDate(data.getStartDate() != null ? data.getStartDate() : ep.getStartDate());
            ep.setEndDate(data.getEndDate());
            ep.setNote(data.getNote());
            ep.setDescription(data.getDescription());
            return repository.save(ep);
        }).orElseThrow(() -> new RuntimeException("EmployeePosition not found"));
    }
    
    public void unassignPosition(UUID employeeId, UUID positionId) {
        repository.findByEmployeeIdAndPositionId(employeeId, positionId)
                  .ifPresent(ep -> repository.delete(ep));
    }
    
    public Optional<EmployeePosition> updateEndDate(UUID id, LocalDateTime endDate) {
        return repository.findById(id).map(ep -> {
            ep.setEndDate(endDate);
            return repository.save(ep);
        });
    }
}
