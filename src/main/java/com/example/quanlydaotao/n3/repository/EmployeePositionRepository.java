package com.example.quanlydaotao.n3.repository;

import com.example.quanlydaotao.n3.entity.EmployeePosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeePositionRepository extends JpaRepository<EmployeePosition, UUID> {
    List<EmployeePosition> findByEmployeeId(UUID employeeId);
    List<EmployeePosition> findByPositionId(UUID positionId);
    Optional<EmployeePosition> findByEmployeeIdAndPositionId(UUID employeeId, UUID positionId);
}
