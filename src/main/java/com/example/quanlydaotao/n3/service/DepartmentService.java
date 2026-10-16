package com.example.quanlydaotao.n3.service;

import com.example.quanlydaotao.n3.entity.Department;
import com.example.quanlydaotao.n3.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DepartmentService {
    @Autowired
    private DepartmentRepository repository;

    public List<Department> findAll() { return repository.findAll(); }
    public Optional<Department> findById(UUID id) { return repository.findById(id); }
    public Department save(Department entity) { return repository.save(entity); }
    public void deleteById(UUID id) { repository.deleteById(id); }
    public List<Department> search(String keyword) { return repository.searchByKeyword(keyword); }
    public Optional<Department> updateStatus(UUID id, boolean status) {
        return repository.findById(id).map(dept -> {
            dept.setIsActive(status);
            return repository.save(dept);
        });
    }
}
