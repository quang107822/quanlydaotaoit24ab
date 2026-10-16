package com.example.quanlydaotao.n1.service;

import com.example.quanlydaotao.n1.entity.Role;
import com.example.quanlydaotao.n1.repository.RoleRepository;
import org.springframework.stereotype.Service;

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
        return roleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy role với ID: " + id));
    }

    @Override
    public Role create(Role role) {

        // Tạo UUID tự động
        role.setId(UUID.randomUUID());

        // Thời gian tạo
        role.setCreatedAt(LocalDateTime.now());

        // Thời gian cập nhật
        role.setUpdatedAt(LocalDateTime.now());

        // Mặc định role đang hoạt động
        if (role.getIsActive() == null) {
            role.setIsActive(true);
        }

        // Mặc định không phải system role
        if (role.getIsSystem() == null) {
            role.setIsSystem(false);
        }

        return roleRepository.save(role);
    }

    @Override
    public Role update(UUID id, Role role) {

        Role existingRole = getById(id);

        existingRole.setCode(role.getCode());
        existingRole.setName(role.getName());
        existingRole.setDescription(role.getDescription());
        existingRole.setIsSystem(role.getIsSystem());
        existingRole.setIsActive(role.getIsActive());

        existingRole.setUpdatedAt(LocalDateTime.now());

        return roleRepository.save(existingRole);
    }

    @Override
    public void delete(UUID id) {

        Role role = getById(id);

        // Xóa mềm
        role.setDeletedAt(LocalDateTime.now());
        role.setIsActive(false);

        roleRepository.save(role);
    }

    @Override
    public Role restore(UUID id) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy role với ID: " + id));

        role.setDeletedAt(null);
        role.setDeletedBy(null);
        role.setIsActive(true);
        role.setUpdatedAt(LocalDateTime.now());

        return roleRepository.save(role);
    }
}