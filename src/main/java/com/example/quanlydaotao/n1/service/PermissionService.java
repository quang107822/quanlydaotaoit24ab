package com.example.quanlydaotao.n1.service;

import com.example.quanlydaotao.n1.entity.Permission;
import com.example.quanlydaotao.n1.repository.PermissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Nghiệp vụ quản lý danh mục quyền chức năng (bảng permissions).
 *
 * Quy ước:
 * - "code" là mã quyền (unique trong DB), "name" là tên hiển thị (không unique).
 * - Xoá là xoá mềm: mọi truy vấn lấy dữ liệu đều lọc deleted_at IS NULL.
 * - Lỗi nghiệp vụ trả về ResponseStatusException để API có đúng HTTP status
 *   (400/404/409) thay vì luôn 500.
 */
@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    // ==================== READ ====================

    /** Lấy tất cả quyền chưa bị xoá mềm. */
    public List<Permission> getAllPermissions() {
        return permissionRepository.findByDeletedAtIsNull();
    }

    /** Tìm kiếm quyền theo code / name / module; keyword rỗng thì trả về tất cả. */
    public List<Permission> searchPermissions(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllPermissions();
        }
        return permissionRepository.searchActivePermissions(keyword.trim());
    }

    /** Lấy quyền đang hoạt động. */
    public List<Permission> getActivePermissions() {
        return permissionRepository.findByIsActiveAndDeletedAtIsNull(true);
    }

    /** Lấy quyền theo nhóm chức năng. */
    public List<Permission> getByModule(String module) {
        String value = normalizeText(module);
        if (value == null) {
            throw badRequest("Module không được để trống");
        }
        return permissionRepository.findByModuleIgnoreCaseAndDeletedAtIsNull(value);
    }

    /**
     * Lấy quyền theo tên hiển thị.
     * name không unique trong DB nên trả về danh sách (tránh NonUniqueResultException).
     */
    public List<Permission> getByName(String name) {
        String value = normalizeText(name);
        if (value == null) {
            throw badRequest("Tên quyền không được để trống");
        }
        return permissionRepository.findByNameIgnoreCaseAndDeletedAtIsNull(value);
    }

    /** Lấy quyền theo mã quyền (code). */
    public Permission getByCode(String code) {
        String value = normalizeKey(code);
        if (value == null) {
            throw badRequest("Mã quyền (code) không được để trống");
        }
        return permissionRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(value)
                .orElseThrow(() -> notFound("Không tìm thấy permission với code: " + value));
    }

    /** Lấy quyền theo id. */
    public Permission getPermissionById(UUID id) {
        if (id == null) {
            throw badRequest("Id permission không được để trống");
        }
        return permissionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> notFound("Không tìm thấy permission: " + id));
    }

    // ==================== WRITE ====================

    /** Thêm quyền mới. */
    @Transactional
    public Permission createPermission(Permission request) {

        if (request == null) {
            throw badRequest("Dữ liệu permission không được để trống");
        }

        String code = normalizeKey(request.getCode());
        String name = normalizeText(request.getName());

        if (code == null) {
            throw badRequest("Mã quyền (code) là thông tin bắt buộc");
        }
        if (name == null) {
            throw badRequest("Tên quyền (name) là thông tin bắt buộc");
        }

        // UQ_permissions_code áp cho cả bản ghi đã xoá mềm => phải kiểm tra không lọc deletedAt
        permissionRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (existing.getDeletedAt() != null) {
                throw conflict("Mã quyền '" + code + "' đang thuộc bản ghi đã xoá mềm ("
                        + existing.getId() + "). Hãy restore bản ghi đó hoặc dùng mã khác.");
            }
            throw conflict("Mã quyền đã tồn tại: " + code);
        });

        LocalDateTime now = LocalDateTime.now();

        Permission permission = new Permission();
        // Luôn sinh id mới, không dùng id do client gửi lên (tránh ghi đè bản ghi khác)
        permission.setId(UUID.randomUUID());
        permission.setCode(code);
        permission.setName(name);
        permission.setModule(normalizeKey(request.getModule()));
        permission.setDescription(normalizeText(request.getDescription()));
        permission.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        permission.setCreatedAt(now);
        permission.setUpdatedAt(now);
        permission.setCreatedBy(request.getCreatedBy());

        return permissionRepository.save(permission);
    }

    /**
     * Cập nhật quyền. Chỉ cập nhật các trường được gửi lên (null = giữ nguyên),
     * tránh việc PUT thiếu field làm mất dữ liệu.
     */
    @Transactional
    public Permission updatePermission(UUID id, Permission request) {

        if (request == null) {
            throw badRequest("Dữ liệu cập nhật không được để trống");
        }

        Permission permission = getPermissionById(id);

        if (request.getCode() != null) {
            String code = normalizeKey(request.getCode());
            if (code == null) {
                throw badRequest("Mã quyền (code) không được để trống");
            }
            if (permissionRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw conflict("Mã quyền đã tồn tại: " + code);
            }
            permission.setCode(code);
        }

        if (request.getName() != null) {
            String name = normalizeText(request.getName());
            if (name == null) {
                throw badRequest("Tên quyền (name) không được để trống");
            }
            permission.setName(name);
        }

        // Gửi chuỗi rỗng => xoá giá trị (module/description cho phép null)
        if (request.getModule() != null) {
            permission.setModule(normalizeKey(request.getModule()));
        }
        if (request.getDescription() != null) {
            permission.setDescription(normalizeText(request.getDescription()));
        }
        if (request.getIsActive() != null) {
            permission.setIsActive(request.getIsActive());
        }

        permission.setUpdatedAt(LocalDateTime.now());
        if (request.getUpdatedBy() != null) {
            permission.setUpdatedBy(request.getUpdatedBy());
        }

        return permissionRepository.save(permission);
    }

    /** Bật / tắt quyền. */
    @Transactional
    public Permission updateActive(UUID id, Boolean active) {

        if (active == null) {
            throw badRequest("Trạng thái active là thông tin bắt buộc");
        }

        Permission permission = getPermissionById(id);
        LocalDateTime now = LocalDateTime.now();

        permission.setIsActive(active);
        permission.setUpdatedAt(now);

        return permissionRepository.save(permission);
    }

    /** Xoá mềm quyền. */
    @Transactional
    public void deletePermission(UUID id) {

        Permission permission = getPermissionById(id);
        LocalDateTime now = LocalDateTime.now();

        permission.setDeletedAt(now);
        permission.setUpdatedAt(now);
        permission.setIsActive(false);

        permissionRepository.save(permission);
    }

    /** Khôi phục quyền đã xoá mềm. */
    @Transactional
    public Permission restorePermission(UUID id) {

        if (id == null) {
            throw badRequest("Id permission không được để trống");
        }

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy permission: " + id));

        if (permission.getDeletedAt() == null) {
            throw conflict("Permission chưa bị xoá nên không cần khôi phục");
        }

        permission.setDeletedAt(null);
        permission.setDeletedBy(null);
        permission.setIsActive(true);
        permission.setUpdatedAt(LocalDateTime.now());

        return permissionRepository.save(permission);
    }

    // ==================== HELPERS ====================

    /** Chuẩn hoá mã (code/module): trim + upper case, rỗng => null. */
    private static String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    /** Chuẩn hoá text thường (name/description): trim, rỗng => null. */
    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
