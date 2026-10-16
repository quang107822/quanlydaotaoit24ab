package com.example.quanlydaotao.n3.repository;

import com.example.quanlydaotao.n3.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    @Query("SELECT e FROM Employee e WHERE e.code LIKE %:keyword% OR e.fullName LIKE %:keyword% OR e.email LIKE %:keyword% OR e.phone LIKE %:keyword%")
    List<Employee> searchByKeyword(@Param("keyword") String keyword);
    
    List<Employee> findByDepartmentId(UUID departmentId);
}
