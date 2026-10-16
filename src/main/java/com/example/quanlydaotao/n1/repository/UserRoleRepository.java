package com.example.quanlydaotao.n1.repository;

import com.example.quanlydaotao.n1.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    // Tất cả phân quyền chưa bị xoá mềm
    List<UserRole> findByDeletedAtIsNull();

    // Một phân quyền theo id, chỉ lấy bản ghi chưa xoá mềm
    Optional<UserRole> findByIdAndDeletedAtIsNull(UUID id);

    // Các phân quyền của 1 user
    List<UserRole> findByUserIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID userId);

    List<UserRole> findByUserIdAndIsActiveAndDeletedAtIsNull(UUID userId, Boolean isActive);

    // Các phân quyền của 1 role
    List<UserRole> findByRoleIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID roleId);

    // Bảng không có unique (user_id, role_id) nên dùng findFirst để tránh NonUniqueResultException
    Optional<UserRole> findFirstByUserIdAndRoleIdAndDeletedAtIsNull(UUID userId, UUID roleId);

    // Bản ghi đã xoá mềm gần nhất của 1 cặp (user, role) - dùng để khôi phục thay vì tạo mới
    Optional<UserRole> findFirstByUserIdAndRoleIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(UUID userId, UUID roleId);

    boolean existsByUserIdAndRoleIdAndDeletedAtIsNull(UUID userId, UUID roleId);

    long countByRoleIdAndDeletedAtIsNull(UUID roleId);
}
