package com.example.quanlydaotao.n6.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.quanlydaotao.n6.entity.EmployeeLookup;

public interface EmployeeLookupRepository extends JpaRepository<EmployeeLookup, UUID> {

    Optional<EmployeeLookup> findByIdAndDeletedAtIsNull(UUID id);

    List<EmployeeLookup> findByDeletedAtIsNullOrderByCodeAsc();
}
