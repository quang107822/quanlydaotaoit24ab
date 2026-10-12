package com.example.quanlydaotao.service;

import com.example.quanlydaotao.entity.Role;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    List<Role> getAll();

    Role getById(UUID id);

    Role create(Role role);

    Role update(UUID id, Role role);

    void delete(UUID id);

    Role restore(UUID id);
}