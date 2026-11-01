package com.example.quanlydaotao.n6.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.quanlydaotao.n6.entity.CourseRegistration;

public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, UUID> {

    long countByRegistrationPeriodIdAndDeletedAtIsNull(UUID registrationPeriodId);

    long countByRegistrationPeriodIdAndStatusAndDeletedAtIsNull(UUID registrationPeriodId, Integer status);

    long countByRegistrationPeriodIdAndStatusAndIsPaidTrueAndDeletedAtIsNull(
            UUID registrationPeriodId,
            Integer status);

    long countByEcourseSectionIdAndStatusAndDeletedAtIsNull(UUID sectionId, Integer status);

    long countByEcourseSectionIdAndRegistrationPeriodIdAndStudentIdAndStatusAndDeletedAtIsNull(
            UUID sectionId,
            UUID periodId,
            UUID studentId,
            Integer status);

    Optional<CourseRegistration> findByIdAndDeletedAtIsNull(UUID id);

    List<CourseRegistration> findByEcourseSectionIdAndStatusAndDeletedAtIsNullOrderByRegisteredAtDesc(
            UUID sectionId,
            Integer status);

    List<CourseRegistration> findByRegistrationPeriodIdAndDeletedAtIsNullOrderByRegisteredAtAsc(UUID periodId);

    @Query("""
            select count(distinct r.studentId)
            from CourseRegistration r
            where r.registrationPeriodId = :periodId
              and r.status = 1
              and r.deletedAt is null
            """)
    long countDistinctRegisteredStudents(@Param("periodId") UUID periodId);

    @Query("""
            select r from CourseRegistration r
            where r.registrationPeriodId = :periodId
              and r.status = 1
              and r.deletedAt is null
            order by r.registeredAt
            """)
    List<CourseRegistration> findActiveByPeriod(@Param("periodId") UUID periodId);

    long countByEcourseSectionIdAndRegistrationPeriodIdAndStatusAndIsPaidTrueAndDeletedAtIsNull(
            UUID sectionId,
            UUID periodId,
            Integer status);
}
