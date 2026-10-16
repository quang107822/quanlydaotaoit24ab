package com.example.quanlydaotao.n1.repository;

import com.example.quanlydaotao.n1.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    // ===== Lấy danh sách quyền chưa bị xoá mềm =====
    List<Permission> findByDeletedAtIsNull();

    // Lấy 1 quyền theo id, chỉ lấy bản ghi chưa xoá mềm
    Optional<Permission> findByIdAndDeletedAtIsNull(UUID id);

    // ===== Truy vấn theo code =====
    // code là UNIQUE trong DB (UQ_permissions_code) và ràng buộc này áp cho CẢ bản ghi
    // đã xoá mềm, nên cần cả bản có/không lọc deletedAt.
    Optional<Permission> findByCodeIgnoreCase(String code);

    Optional<Permission> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    // ===== Truy vấn khác =====
    // name KHÔNG unique trong DB nên trả về List để tránh NonUniqueResultException
    List<Permission> findByNameIgnoreCaseAndDeletedAtIsNull(String name);

    List<Permission> findByModuleIgnoreCaseAndDeletedAtIsNull(String module);

    List<Permission> findByIsActiveAndDeletedAtIsNull(Boolean isActive);

    // Tìm kiếm theo code / name / module
    @Query("""
            select p from Permission p
            where p.deletedAt is null
              and (
                lower(p.code) like lower(concat('%', :keyword, '%'))
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(p.module) like lower(concat('%', :keyword, '%'))
              )
            order by p.module asc, p.code asc
            """)
    List<Permission> searchActivePermissions(@Param("keyword") String keyword);
}
