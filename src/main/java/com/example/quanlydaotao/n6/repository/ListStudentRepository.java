package com.example.quanlydaotao.n6.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.quanlydaotao.n6.entity.ListStudent;

public interface ListStudentRepository extends JpaRepository<ListStudent, UUID> {

    List<ListStudent> findByRegistrationPeriodIdAndIsActiveTrueAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID registrationPeriodId);

    Optional<ListStudent> findByIdAndRegistrationPeriodIdAndDeletedAtIsNull(UUID id, UUID registrationPeriodId);

    Optional<ListStudent> findByRegistrationPeriodIdAndStudentIdAndDeletedAtIsNull(
            UUID registrationPeriodId,
            UUID studentId);

    boolean existsByRegistrationPeriodIdAndStudentIdAndIsActiveTrueAndDeletedAtIsNull(
            UUID registrationPeriodId,
            UUID studentId);

    long countByRegistrationPeriodIdAndIsActiveTrueAndDeletedAtIsNull(UUID registrationPeriodId);
}
