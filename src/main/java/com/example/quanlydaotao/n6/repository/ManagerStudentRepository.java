package com.example.quanlydaotao.n6.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.quanlydaotao.n6.entity.ManagerStudent;

public interface ManagerStudentRepository extends JpaRepository<ManagerStudent, UUID> {

    List<ManagerStudent> findByDeletedAtIsNullOrderByCreatedAtDesc();

    Optional<ManagerStudent> findByIdAndDeletedAtIsNull(UUID id);

    List<ManagerStudent> findByStudentIdAndRoleTypeAndIsPrimaryTrueAndDeletedAtIsNull(
            UUID studentId,
            Integer roleType);
}
