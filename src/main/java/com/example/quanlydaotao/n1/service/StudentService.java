package com.example.quanlydaotao.n1.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.quanlydaotao.n1.entity.Student;
import com.example.quanlydaotao.n1.repository.StudentRepository;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public List<Student> getAll() {
        return studentRepository.findAll();
    }

    public List<Student> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return studentRepository.findAll();
        }
        String value = keyword.trim();
        return studentRepository.findByStudentCodeContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(
                value, value, value, value);
    }

    public Student getById(UUID id) {
        if (id == null) {
            throw badRequest("Id sinh viên không được để trống");
        }
        return studentRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy sinh viên với id: " + id));
    }

    /** Thêm sinh viên mới. */
    @Transactional
    public Student create(Student student) {

        if (student == null) {
            throw badRequest("Dữ liệu sinh viên không được để trống");
        }

        // Các cột NOT NULL trong bảng students
        String studentCode = normalize(student.getStudentCode());
        String fullName = normalize(student.getFullName());
        String email = normalize(student.getEmail());
        String phone = normalize(student.getPhone());
        String className = normalize(student.getClassName());

        if (studentCode == null) {
            throw badRequest("Mã sinh viên (studentCode) là thông tin bắt buộc");
        }
        if (fullName == null) {
            throw badRequest("Họ tên (fullName) là thông tin bắt buộc");
        }
        if (email == null) {
            throw badRequest("Email là thông tin bắt buộc");
        }
        if (phone == null) {
            throw badRequest("Số điện thoại (phone) là thông tin bắt buộc");
        }
        if (className == null) {
            throw badRequest("Lớp (className) là thông tin bắt buộc");
        }

        Student entity = new Student(studentCode, fullName, email, phone, className);
        entity.setId(null); // để Hibernate tự sinh id

        return studentRepository.save(entity);
    }

    /**
     * Cập nhật sinh viên phải tồn tại trước đó (tránh việc PUT lại tạo bản ghi mới),
     * chỉ cập nhật các trường được gửi lên.
     */
    @Transactional
    public Student update(UUID id, Student request) {

        if (request == null) {
            throw badRequest("Dữ liệu cập nhật không được để trống");
        }

        Student student = getById(id);

        if (request.getStudentCode() != null) {
            student.setStudentCode(requireValue(request.getStudentCode(), "Mã sinh viên (studentCode)"));
        }
        if (request.getFullName() != null) {
            student.setFullName(requireValue(request.getFullName(), "Họ tên (fullName)"));
        }
        if (request.getEmail() != null) {
            student.setEmail(requireValue(request.getEmail(), "Email"));
        }
        if (request.getPhone() != null) {
            student.setPhone(requireValue(request.getPhone(), "Số điện thoại (phone)"));
        }
        if (request.getClassName() != null) {
            student.setClassName(requireValue(request.getClassName(), "Lớp (className)"));
        }

        return studentRepository.save(student);
    }

    @Transactional
    public void delete(UUID id) {
        // getById để trả 404 nếu không tồn tại (deleteById trước đây gây lỗi 500)
        Student student = getById(id);
        studentRepository.delete(student);
    }

    // ==================== HELPERS ====================

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireValue(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw badRequest(fieldName + " không được để trống");
        }
        return normalized;
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
