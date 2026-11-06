package com.example.quanlydaotao.n6.service;

import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.EcourseSectionRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.EcourseSectionResponse;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.PageResult;
import com.example.quanlydaotao.n6.entity.CourseLookup;
import com.example.quanlydaotao.n6.entity.CourseRegistration;
import com.example.quanlydaotao.n6.entity.EcourseSection;
import com.example.quanlydaotao.n6.repository.CourseLookupRepository;
import com.example.quanlydaotao.n6.repository.CourseRegistrationRepository;
import com.example.quanlydaotao.n6.repository.EcourseSectionRepository;
import com.example.quanlydaotao.n6.repository.SemesterLookupRepository;

@Service
public class EcourseSectionService {

    private final EcourseSectionRepository sectionRepository;
    private final CourseLookupRepository courseRepository;
    private final SemesterLookupRepository semesterRepository;
    private final CourseRegistrationRepository registrationRepository;

    public EcourseSectionService(
            EcourseSectionRepository sectionRepository,
            CourseLookupRepository courseRepository,
            SemesterLookupRepository semesterRepository,
            CourseRegistrationRepository registrationRepository) {
        this.sectionRepository = sectionRepository;
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<EcourseSectionResponse> search(
            UUID semesterId,
            Integer status,
            UUID courseId,
            String keyword,
            int page,
            int size) {
        validatePage(page, size);
        Page<EcourseSection> result = sectionRepository.search(
                semesterId,
                status,
                courseId,
                normalize(keyword),
                PageRequest.of(page, size, Sort.by("code").ascending()));
        return new PageResult<>(result.getTotalElements(), page, size, result.getContent().stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public EcourseSectionResponse get(UUID id) {
        return toResponse(findActive(id));
    }

    @Transactional
    public EcourseSectionResponse create(EcourseSectionRequest request) {
        validate(request);
        if (sectionRepository.existsBySemesterIdAndCodeIgnoreCaseAndDeletedAtIsNull(
                request.semesterId(), request.code().trim())) {
            throw conflict("Mã lớp học phần đã tồn tại trong học kỳ này.");
        }

        EcourseSection section = new EcourseSection();
        apply(section, request);
        return toResponse(sectionRepository.save(section));
    }

    @Transactional
    public EcourseSectionResponse update(UUID id, EcourseSectionRequest request) {
        validate(request);
        EcourseSection section = sectionRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> notFound("Không tìm thấy lớp học phần."));
        if (sectionRepository.existsBySemesterIdAndCodeIgnoreCaseAndDeletedAtIsNullAndIdNot(
                request.semesterId(), request.code().trim(), id)) {
            throw conflict("Mã lớp học phần đã tồn tại trong học kỳ này.");
        }
        if (request.maxCapacity() < section.getCurrentEnrolled()) {
            throw conflict("Sĩ số tối đa không được nhỏ hơn số sinh viên đang đăng ký.");
        }
        apply(section, request);
        return toResponse(sectionRepository.save(section));
    }

    @Transactional
    public EcourseSectionResponse changeStatus(UUID id, int status) {
        if (status < 1 || status > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái phải là 1, 2 hoặc 3.");
        }
        EcourseSection section = sectionRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> notFound("Không tìm thấy lớp học phần."));
        section.setStatus(status);
        if (status == 2) {
            for (CourseRegistration registration : registrationRepository
                    .findByEcourseSectionIdAndStatusAndDeletedAtIsNullOrderByRegisteredAtDesc(id, 1)) {
                registration.setStatus(3);
            }
            section.setCurrentEnrolled(0);
        }
        return toResponse(sectionRepository.save(section));
    }

    private void validate(EcourseSectionRequest request) {
        if (request.maxCapacity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sĩ số tối đa phải lớn hơn 0.");
        }
        if (request.status() != null && (request.status() < 1 || request.status() > 3)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái phải là 1, 2 hoặc 3.");
        }
        courseRepository.findActiveById(request.courseId())
                .orElseThrow(() -> notFound("Không tìm thấy môn học hoặc môn học đã ngừng hoạt động."));
        semesterRepository.findActiveById(request.semesterId())
                .orElseThrow(() -> notFound("Không tìm thấy học kỳ hoặc học kỳ đã ngừng hoạt động."));
    }

    private void apply(EcourseSection section, EcourseSectionRequest request) {
        CourseLookup course = courseRepository.findActiveById(request.courseId())
                .orElseThrow(() -> notFound("Không tìm thấy môn học hoặc môn học đã ngừng hoạt động."));
        int credits;
        try {
            credits = course.getCredits().setScale(0, RoundingMode.UNNECESSARY).intValueExact();
        } catch (ArithmeticException | NullPointerException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Số tín chỉ của môn học phải là số nguyên hợp lệ.",
                    exception);
        }

        section.setCourseId(course.getId());
        section.setSemesterId(request.semesterId());
        section.setCode(request.code().trim());
        section.setCourseCode(course.getCode());
        section.setCourseName(course.getName());
        section.setCredits(credits);
        section.setMaxCapacity(request.maxCapacity());
        if (request.status() != null) {
            section.setStatus(request.status());
        }
        if (section.getCurrentEnrolled() == null) {
            section.setCurrentEnrolled(0);
        }
    }

    private EcourseSection findActive(UUID id) {
        return sectionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> notFound("Không tìm thấy lớp học phần."));
    }

    private EcourseSectionResponse toResponse(EcourseSection section) {
        String semesterName = semesterRepository.findById(section.getSemesterId())
                .map(semester -> semester.getName())
                .orElse(null);
        return new EcourseSectionResponse(
                section.getId(),
                section.getCourseId(),
                section.getSemesterId(),
                semesterName,
                section.getCode(),
                section.getCourseCode(),
                section.getCourseName(),
                section.getCredits(),
                section.getMaxCapacity(),
                section.getCurrentEnrolled(),
                section.getStatus(),
                section.getRowVersion());
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phân trang yêu cầu page >= 0 và size từ 1 đến 100.");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
