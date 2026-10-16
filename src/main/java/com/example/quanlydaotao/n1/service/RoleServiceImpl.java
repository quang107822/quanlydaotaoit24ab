package com.example.quanlydaotao.n1.service;

import com.example.quanlydaotao.n1.entity.Role;
import com.example.quanlydaotao.n1.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public List<Role> getAll() {
        return roleRepository.findByDeletedAtIsNull();
    }

    @Override
    public Role getById(UUID id) {
        if (id == null) {
            throw badRequest("Id role không được để trống");
        }
        return roleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        notFound("Không tìm thấy role với ID: " + id));
    }

    @Override
    @Transactional
    public Role create(Role role) {

        if (role == null) {
            throw badRequest("Dữ liệu role không được để trống");
        }

        String code = normalizeText(role.getCode());
        String name = normalizeText(role.getName());

        if (code == null) {
            throw badRequest("Mã role (code) là thông tin bắt buộc");
        }
        if (name == null) {
            throw badRequest("Tên role (name) là thông tin bắt buộc");
        }

        // UQ_roles_code áp cho cả bản ghi đã xoá mềm => kiểm tra không lọc deletedAt
        roleRepository.findByCodeIgnoreCase(code).ifPresent(existing -> {
            if (existing.getDeletedAt() != null) {
                throw conflict("Mã role '" + code + "' đang thuộc bản ghi đã xoá mềm ("
                        + existing.getId() + "). Hãy khôi phục bản ghi đó hoặc dùng mã khác.");
            }
            throw conflict("Mã role đã tồn tại: " + code);
        });

        LocalDateTime now = LocalDateTime.now();

        // Tạo UUID tự động, không dùng id do client gửi lên
        role.setId(UUID.randomUUID());
        role.setCode(code);
        role.setName(name);
        role.setDescription(normalizeText(role.getDescription()));
        role.setCreatedAt(now);
        role.setUpdatedAt(now);

        if (role.getIsActive() == null) {
            role.setIsActive(true);
        }
        if (role.getIsSystem() == null) {
            role.setIsSystem(false);
        }

        return roleRepository.save(role);
    }

    /**
     * Cập nhật role. Chỉ cập nhật các trường được gửi lên (null = giữ nguyên)
     * để tránh việc PUT thiếu field làm mất dữ liệu.
     */
    @Override
    @Transactional
    public Role update(UUID id, Role role) {

        if (role == null) {
            throw badRequest("Dữ liệu cập nhật không được để trống");
        }

        Role existingRole = getById(id);

        if (role.getCode() != null) {
            String code = normalizeText(role.getCode());
            if (code == null) {
                throw badRequest("Mã role (code) không được để trống");
            }
            if (roleRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
                throw conflict("Mã role đã tồn tại: " + code);
            }
            existingRole.setCode(code);
        }

        if (role.getName() != null) {
            String name = normalizeText(role.getName());
            if (name == null) {
                throw badRequest("Tên role (name) không được để trống");
            }
            existingRole.setName(name);
        }

        // Gửi chuỗi rỗng => xoá mô tả
        if (role.getDescription() != null) {
            existingRole.setDescription(normalizeText(role.getDescription()));
        }
        if (role.getIsSystem() != null) {
            existingRole.setIsSystem(role.getIsSystem());
        }
        if (role.getIsActive() != null) {
            existingRole.setIsActive(role.getIsActive());
        }
        if (role.getUpdatedBy() != null) {
            existingRole.setUpdatedBy(role.getUpdatedBy());
        }

        existingRole.setUpdatedAt(LocalDateTime.now());

        return roleRepository.save(existingRole);
    }

    @Override
    @Transactional
    public void delete(UUID id) {

        Role role = getById(id);
        LocalDateTime now = LocalDateTime.now();

        // Xóa mềm
        role.setDeletedAt(now);
        role.setUpdatedAt(now);
        role.setIsActive(false);

        roleRepository.save(role);
    }

    @Override
    @Transactional
    public Role restore(UUID id) {

        if (id == null) {
            throw badRequest("Id role không được để trống");
        }

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        notFound("Không tìm thấy role với ID: " + id));

        if (role.getDeletedAt() == null) {
            throw conflict("Role chưa bị xoá nên không cần khôi phục");
        }

        role.setDeletedAt(null);
        role.setDeletedBy(null);
        role.setIsActive(true);
        role.setUpdatedAt(LocalDateTime.now());

        return roleRepository.save(role);
    }

    // ==================== HELPERS ====================

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
