package com.example.quanlydaotao.n3.repository;

import com.example.quanlydaotao.n3.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {
    @Query("SELECT p FROM Position p WHERE p.code LIKE %:keyword% OR p.title LIKE %:keyword%")
    List<Position> searchByKeyword(@Param("keyword") String keyword);
    
    List<Position> findByDepartmentId(UUID departmentId);
}
