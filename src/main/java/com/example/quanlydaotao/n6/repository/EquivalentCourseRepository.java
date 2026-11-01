package com.example.quanlydaotao.n6.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.quanlydaotao.n6.entity.EquivalentCourse;

public interface EquivalentCourseRepository extends JpaRepository<EquivalentCourse, UUID> {

    List<EquivalentCourse> findByIsActiveTrueAndDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<EquivalentCourse> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByOriginalCourseIdAndEquivalentCourseIdAndIsActiveTrueAndDeletedAtIsNull(
            UUID originalCourseId,
            UUID equivalentCourseId);

    boolean existsByOriginalCourseIdAndEquivalentCourseIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
            UUID originalCourseId,
            UUID equivalentCourseId,
            UUID id);
}
