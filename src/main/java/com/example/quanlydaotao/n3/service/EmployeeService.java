package com.example.quanlydaotao.n3.service;

import com.example.quanlydaotao.n3.entity.Employee;
import com.example.quanlydaotao.n3.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmployeeService {
    @Autowired
    private EmployeeRepository repository;

    public List<Employee> findAll() { return repository.findAll(); }
    public Optional<Employee> findById(UUID id) { return repository.findById(id); }
    public Employee save(Employee entity) { return repository.save(entity); }
    public void deleteById(UUID id) { repository.deleteById(id); }
    public List<Employee> search(String keyword) { return repository.searchByKeyword(keyword); }
    public List<Employee> findByDepartmentId(UUID deptId) { return repository.findByDepartmentId(deptId); }
    public Optional<Employee> updateStatus(UUID id, boolean status) {
        return repository.findById(id).map(emp -> {
            emp.setIsActive(status);
            return repository.save(emp);
        });
    }
}
