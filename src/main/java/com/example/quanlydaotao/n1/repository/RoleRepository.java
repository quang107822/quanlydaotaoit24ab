package com.example.quanlydaotao.n1.repository;

import com.example.quanlydaotao.n1.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    // Chỉ lấy những role chưa bị xóa
    List<Role> findByDeletedAtIsNull();

    // Tìm role chưa bị xóa
    java.util.Optional<Role> findByIdAndDeletedAtIsNull(UUID id);
}