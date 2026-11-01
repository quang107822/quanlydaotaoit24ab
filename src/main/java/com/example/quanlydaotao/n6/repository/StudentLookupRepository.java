package com.example.quanlydaotao.n6.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.quanlydaotao.n6.entity.StudentLookup;

public interface StudentLookupRepository extends JpaRepository<StudentLookup, UUID> {

    Optional<StudentLookup> findFirstByCodeIgnoreCase(String code);

    Optional<StudentLookup> findFirstById(UUID id);

    List<StudentLookup> findAllByOrderByCodeAsc();
}
