package com.example.quanlydaotao.n3.repository;

import com.example.quanlydaotao.n3.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    @Query("SELECT d FROM Department d WHERE d.code LIKE %:keyword% OR d.name LIKE %:keyword%")
    List<Department> searchByKeyword(@Param("keyword") String keyword);
}
