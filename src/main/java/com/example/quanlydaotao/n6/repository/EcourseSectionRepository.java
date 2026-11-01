package com.example.quanlydaotao.n6.repository;

import com.example.quanlydaotao.n6.entity.EcourseSection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EcourseSectionRepository extends JpaRepository<EcourseSection, UUID> {

    @Query("""
            select s from EcourseSection s
            where s.deletedAt is null
              and (:semesterId is null or s.semesterId = :semesterId)
              and (:status is null or s.status = :status)
              and (:courseId is null or s.courseId = :courseId)
              and (:keyword is null
                   or lower(s.code) like lower(concat('%', :keyword, '%'))
                   or lower(s.courseCode) like lower(concat('%', :keyword, '%'))
                   or lower(s.courseName) like lower(concat('%', :keyword, '%')))
            """)
    Page<EcourseSection> search(
            @Param("semesterId") UUID semesterId,
            @Param("status") Integer status,
            @Param("courseId") UUID courseId,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<EcourseSection> findByIdAndDeletedAtIsNull(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from EcourseSection s where s.id = :id and s.deletedAt is null")
    Optional<EcourseSection> findActiveByIdForUpdate(@Param("id") UUID id);

    boolean existsBySemesterIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID semesterId, String code);

    boolean existsBySemesterIdAndCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(
            UUID semesterId,
            String code,
            UUID id);

    List<EcourseSection> findByDeletedAtIsNullAndSemesterIdOrderByCode(UUID semesterId);
}