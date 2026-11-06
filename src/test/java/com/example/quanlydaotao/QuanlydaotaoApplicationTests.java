package com.example.quanlydaotao;

import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.quanlydaotao.n6.controller.LookupController;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.ManagerStudentRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.ManagerStudentResponse;
import com.example.quanlydaotao.n6.entity.EmployeeLookup;
import com.example.quanlydaotao.n6.entity.SemesterLookup;
import com.example.quanlydaotao.n6.entity.StudentLookup;
import com.example.quanlydaotao.n6.repository.EmployeeLookupRepository;
import com.example.quanlydaotao.n6.repository.ManagerStudentRepository;
import com.example.quanlydaotao.n6.repository.SemesterLookupRepository;
import com.example.quanlydaotao.n6.repository.StudentLookupRepository;
import com.example.quanlydaotao.n6.service.ManagerStudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.quanlydaotao.n6.QuanlydaotaoApplication;

@SpringBootTest(classes = QuanlydaotaoApplication.class)
@ActiveProfiles("test")
class QuanlydaotaoApplicationTests {

	@Autowired
	private LookupController lookupController;

	@Autowired
	private StudentLookupRepository studentRepository;

	@Autowired
	private EmployeeLookupRepository employeeRepository;

	@Autowired
	private SemesterLookupRepository semesterRepository;

	@Autowired
	private ManagerStudentRepository managerStudentRepository;

	@Autowired
	private ManagerStudentService managerStudentService;

	@BeforeEach
	void setUp() {
		managerStudentRepository.deleteAll();
		semesterRepository.deleteAll();
		studentRepository.deleteAll();
		employeeRepository.deleteAll();

		SemesterLookup semester = new SemesterLookup();
		semester.setId(UUID.fromString("932ba1e7-ca1b-4609-a015-f874f51c0c59"));
		semester.setCode("20261");
		semester.setName("Học kỳ 1 năm học 2026-2027");
		semester.setStartDate(java.time.LocalDate.of(2026, 8, 17));
		semester.setIsActive(true);
		semesterRepository.save(semester);

		StudentLookup student = new StudentLookup();
		student.setId(UUID.fromString("b6341d9e-f32c-5353-869c-4adfe2478e2d"));
		student.setCode("SV26_001");
		student.setFullName("Đậu Xuân Quang");
		studentRepository.save(student);

		EmployeeLookup lecturer = new EmployeeLookup();
		lecturer.setId(UUID.fromString("86f04d4e-40f2-4ed1-b861-7a13c6a9c002"));
		lecturer.setCode("GV001");
		lecturer.setFullName("Nguyễn Minh Giảng");
		employeeRepository.save(lecturer);
	}

	@Test
	void contextLoads() {
	}

	@Test
	void lookupsReturnStudentsAndLecturersAndAssignmentIsSaved() {
		assertEquals("20261", lookupController.semesters().get(0).code());
		assertEquals("Học kỳ 1 năm học 2026-2027", lookupController.semesters().get(0).name());
		assertEquals("SV26_001", lookupController.students().get(0).code());
		assertEquals("Đậu Xuân Quang", lookupController.students().get(0).fullName());
		assertEquals("GV001", lookupController.lecturers().get(0).code());
		assertEquals("Nguyễn Minh Giảng", lookupController.lecturers().get(0).fullName());

		ManagerStudentResponse saved = managerStudentService.assign(new ManagerStudentRequest(
				UUID.fromString("b6341d9e-f32c-5353-869c-4adfe2478e2d"),
				UUID.fromString("86f04d4e-40f2-4ed1-b861-7a13c6a9c002"),
				1,
				true));
		assertEquals("SV26_001", saved.studentCode());
		assertEquals("GV001", saved.managerCode());
		assertEquals(true, saved.isPrimary());

		List<ManagerStudentResponse> assignments = managerStudentService.list();
		assertEquals(1, assignments.size());
		assertEquals("Đậu Xuân Quang", assignments.get(0).studentName());
		assertEquals("Nguyễn Minh Giảng", assignments.get(0).managerName());
	}
}
