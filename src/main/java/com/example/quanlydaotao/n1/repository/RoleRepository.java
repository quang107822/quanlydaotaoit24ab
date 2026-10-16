package com.example.quanlydaotao.n1.repository;

import com.example.quanlydaotao.n1.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    // Chỉ lấy những role chưa bị xóa
    List<Role> findByDeletedAtIsNull();

    // Tìm role chưa bị xóa
    Optional<Role> findByIdAndDeletedAtIsNull(UUID id);

    // code của role là UNIQUE trong DB (UQ_roles_code) và áp cho cả bản ghi đã xoá mềm
    Optional<Role> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
