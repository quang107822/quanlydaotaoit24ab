package com.example.quanlydaotao.n6.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.quanlydaotao.n6.entity.SemesterLookup;

public interface SemesterLookupRepository extends JpaRepository<SemesterLookup, UUID> {

    @Query("""
            select s from SemesterLookup s
            where s.deletedAt is null and (s.isActive is null or s.isActive = true)
            order by s.startDate desc, s.name
            """)
    List<SemesterLookup> findActiveSemesters();

    @Query("""
            select s from SemesterLookup s
            where s.id = :id and s.deletedAt is null and (s.isActive is null or s.isActive = true)
            """)
    Optional<SemesterLookup> findActiveById(UUID id);
}
