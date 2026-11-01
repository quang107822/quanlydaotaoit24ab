package com.example.quanlydaotao.n6.controller;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.CourseOption;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.PersonOption;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.SemesterOption;
import com.example.quanlydaotao.n6.entity.CourseLookup;
import com.example.quanlydaotao.n6.entity.SemesterLookup;
import com.example.quanlydaotao.n6.repository.EmployeeLookupRepository;
import com.example.quanlydaotao.n6.repository.StudentLookupRepository;
import com.example.quanlydaotao.n6.repository.CourseLookupRepository;
import com.example.quanlydaotao.n6.repository.SemesterLookupRepository;

@RestController
@RequestMapping("/api/v1/lookups")
public class LookupController {

    private final CourseLookupRepository courseRepository;
    private final SemesterLookupRepository semesterRepository;
    private final StudentLookupRepository studentRepository;
    private final EmployeeLookupRepository employeeRepository;

    public LookupController(
            CourseLookupRepository courseRepository,
            SemesterLookupRepository semesterRepository,
            StudentLookupRepository studentRepository,
            EmployeeLookupRepository employeeRepository) {
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.studentRepository = studentRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/courses")
    public List<CourseOption> courses() {
        return courseRepository.findActiveCourses().stream()
                .map(course -> new CourseOption(course.getId(), course.getCode(), course.getName(), course.getCredits()))
                .toList();
    }

    @GetMapping("/semesters")
    public List<SemesterOption> semesters() {
        return semesterRepository.findActiveSemesters().stream()
                .map(semester -> new SemesterOption(semester.getId(), semester.getCode(), semester.getName()))
                .toList();
    }

    @GetMapping("/students")
    public List<PersonOption> students() {
        Set<UUID> seenIds = new HashSet<>();
        return studentRepository.findAllByOrderByCodeAsc().stream()
                .filter(student -> seenIds.add(student.getId()))
                .map(student -> new PersonOption(student.getId(), student.getCode(), student.getFullName()))
                .toList();
    }

    @GetMapping("/managers")
    public List<PersonOption> managers() {
        return employeeRepository.findByDeletedAtIsNullOrderByCodeAsc().stream()
                .map(employee -> new PersonOption(employee.getId(), employee.getCode(), employee.getFullName()))
                .toList();
    }
}
