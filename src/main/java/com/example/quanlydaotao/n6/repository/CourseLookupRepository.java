package com.example.quanlydaotao.n6.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.quanlydaotao.n6.entity.CourseLookup;

public interface CourseLookupRepository extends JpaRepository<CourseLookup, UUID> {

    @Query("""
            select c from CourseLookup c
            where c.deletedAt is null and (c.isActive is null or c.isActive = true)
            order by c.code
            """)
    List<CourseLookup> findActiveCourses();

    @Query("""
            select c from CourseLookup c
            where c.id = :id and c.deletedAt is null and (c.isActive is null or c.isActive = true)
            """)
    Optional<CourseLookup> findActiveById(UUID id);
}
