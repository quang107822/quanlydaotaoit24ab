package com.example.quanlydaotao.n3.service;

import com.example.quanlydaotao.n3.entity.Position;
import com.example.quanlydaotao.n3.repository.PositionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PositionService {
    @Autowired
    private PositionRepository repository;

    public List<Position> findAll() { return repository.findAll(); }
    public Optional<Position> findById(UUID id) { return repository.findById(id); }
    public Position save(Position entity) { return repository.save(entity); }
    public void deleteById(UUID id) { repository.deleteById(id); }
    public List<Position> search(String keyword) { return repository.searchByKeyword(keyword); }
    public List<Position> findByDepartmentId(UUID deptId) { return repository.findByDepartmentId(deptId); }
    public Optional<Position> updateStatus(UUID id, boolean status) {
        return repository.findById(id).map(pos -> {
            pos.setIsActive(status);
            return repository.save(pos);
        });
    }
}
