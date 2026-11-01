package com.example.quanlydaotao.n6.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.quanlydaotao.n6.entity.DepartmentLookup;

public interface DepartmentLookupRepository extends JpaRepository<DepartmentLookup, UUID> {

    Optional<DepartmentLookup> findByIdAndDeletedAtIsNull(UUID id);
}
