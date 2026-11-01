package com.example.quanlydaotao.n6.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.CourseRegistrationResponse;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.PaymentStatusRequest;
import com.example.quanlydaotao.n6.entity.CourseRegistration;
import com.example.quanlydaotao.n6.entity.EcourseSection;
import com.example.quanlydaotao.n6.entity.StudentLookup;
import com.example.quanlydaotao.n6.repository.CourseRegistrationRepository;
import com.example.quanlydaotao.n6.repository.EcourseSectionRepository;
import com.example.quanlydaotao.n6.repository.StudentLookupRepository;

@Service
public class CourseRegistrationQueryService {

    private final CourseRegistrationRepository registrationRepository;
    private final EcourseSectionRepository sectionRepository;
    private final StudentLookupRepository studentRepository;

    public CourseRegistrationQueryService(
            CourseRegistrationRepository registrationRepository,
            EcourseSectionRepository sectionRepository,
            StudentLookupRepository studentRepository) {
        this.registrationRepository = registrationRepository;
        this.sectionRepository = sectionRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseRegistrationResponse> listBySection(UUID sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học phần.");
        }
        return registrationRepository
                .findByEcourseSectionIdAndStatusAndDeletedAtIsNullOrderByRegisteredAtDesc(sectionId, 1)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void updatePayment(UUID id, PaymentStatusRequest request) {
        CourseRegistration registration = registrationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký."));
        if (registration.getStatus() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ có thể cập nhật học phí cho đăng ký đang còn hiệu lực.");
        }
        registration.setIsPaid(request.isPaid());
        registrationRepository.save(registration);
    }

    private CourseRegistrationResponse toResponse(CourseRegistration registration) {
        StudentLookup student = studentRepository.findFirstById(registration.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Đăng ký đang tham chiếu tới sinh viên không còn tồn tại."));
        EcourseSection section = registration.getEcourseSectionId() == null
                ? null
                : sectionRepository.findById(registration.getEcourseSectionId()).orElse(null);
        return new CourseRegistrationResponse(
                registration.getId(),
                registration.getRegistrationPeriodId(),
                student.getId(),
                student.getCode(),
                student.getFullName(),
                registration.getEcourseSectionId(),
                section == null ? null : section.getCode(),
                section == null ? null : section.getCourseCode(),
                section == null ? null : section.getCourseName(),
                registration.getRegistrationType(),
                registration.getReplacedGradeId(),
                registration.getIsPaid(),
                registration.getStatus(),
                registration.getRegisteredAt());
    }
}
