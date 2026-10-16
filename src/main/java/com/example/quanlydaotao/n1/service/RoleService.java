package com.example.quanlydaotao.n1.service;

import com.example.quanlydaotao.n1.entity.Role;

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