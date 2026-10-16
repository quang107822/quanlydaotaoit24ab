package com.example.quanlydaotao.n1.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.quanlydaotao.n1.entity.Student;
import com.example.quanlydaotao.n1.service.StudentService;

@RestController
@RequestMapping("/api/students")
@CrossOrigin("*")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // GET ALL / SEARCH
    @GetMapping
    public ResponseEntity<List<Student>> listStudents(
            @RequestParam(required = false) String keyword) {

        return ResponseEntity.ok(
                studentService.search(keyword)
        );
    }

    // GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudent(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                studentService.getById(id)
        );
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Student> createStudent(
            @RequestBody Student student) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(studentService.create(student));
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(
            @PathVariable UUID id,
            @RequestBody Student student) {

        return ResponseEntity.ok(
                studentService.update(id, student)
        );
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(
            @PathVariable UUID id) {

        studentService.delete(id);

        return ResponseEntity.ok("Xóa sinh viên thành công");
    }
}
