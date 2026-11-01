package com.example.quanlydaotao.n6.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.PageResult;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.RegistrationPeriodRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.RegistrationPeriodResponse;
import com.example.quanlydaotao.n6.entity.RegistrationPeriod;
import com.example.quanlydaotao.n6.repository.CourseRegistrationRepository;
import com.example.quanlydaotao.n6.repository.RegistrationPeriodRepository;
import com.example.quanlydaotao.n6.repository.SemesterLookupRepository;

@Service
public class RegistrationPeriodService {

    private final RegistrationPeriodRepository periodRepository;
    private final SemesterLookupRepository semesterRepository;
    private final CourseRegistrationRepository registrationRepository;

    public RegistrationPeriodService(
            RegistrationPeriodRepository periodRepository,
            SemesterLookupRepository semesterRepository,
            CourseRegistrationRepository registrationRepository) {
        this.periodRepository = periodRepository;
        this.semesterRepository = semesterRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<RegistrationPeriodResponse> search(
            UUID semesterId,
            Boolean isActive,
            Boolean isOpen,
            String keyword,
            int page,
            int size) {
        validatePage(page, size);
        Page<RegistrationPeriod> result = periodRepository.search(
                semesterId,
                isActive,
                isOpen,
                normalize(keyword),
                PageRequest.of(page, size, Sort.by("startTime").descending()));
        return new PageResult<>(result.getTotalElements(), page, size, result.getContent().stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public RegistrationPeriodResponse get(UUID id) {
        return toResponse(findActive(id));
    }

    @Transactional
    public RegistrationPeriodResponse create(RegistrationPeriodRequest request) {
        validate(request, null);
        RegistrationPeriod period = new RegistrationPeriod();
        apply(period, request);
        return toResponse(periodRepository.save(period));
    }

    @Transactional
    public RegistrationPeriodResponse update(UUID id, RegistrationPeriodRequest request) {
        RegistrationPeriod period = findActive(id);
        validate(request, id);
        apply(period, request);
        return toResponse(periodRepository.save(period));
    }

    @Transactional
    public RegistrationPeriodResponse toggle(UUID id, boolean isOpen) {
        RegistrationPeriod period = findActive(id);
        period.setIsOpen(isOpen);
        return toResponse(periodRepository.save(period));
    }

    @Transactional
    public void delete(UUID id) {
        RegistrationPeriod period = findActive(id);
        if (registrationRepository.countByRegistrationPeriodIdAndDeletedAtIsNull(id) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể xóa đợt đăng ký đã có dữ liệu ghi danh.");
        }
        period.setIsOpen(false);
        period.setIsActive(false);
        period.setDeletedAt(LocalDateTime.now());
        periodRepository.save(period);
    }

    private void validate(RegistrationPeriodRequest request, UUID excludeId) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian bắt đầu phải trước thời gian kết thúc.");
        }
        int minCredits = request.minCredits() == null ? 12 : request.minCredits();
        int maxCredits = request.maxCredits() == null ? 25 : request.maxCredits();
        if (minCredits < 0 || maxCredits < 1 || minCredits > maxCredits) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giới hạn tín chỉ không hợp lệ.");
        }
        semesterRepository.findActiveById(request.semesterId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy học kỳ hoặc học kỳ đã ngừng hoạt động."));
        if (periodRepository.overlaps(
                request.semesterId(),
                request.startTime(),
                request.endTime(),
                excludeId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Thời gian đăng ký bị chồng lấp với một đợt khác trong cùng học kỳ.");
        }
    }

    private void apply(RegistrationPeriod period, RegistrationPeriodRequest request) {
        period.setName(request.name().trim());
        period.setSemesterId(request.semesterId());
        period.setStartTime(request.startTime());
        period.setEndTime(request.endTime());
        period.setMinCredits(request.minCredits() == null ? 12 : request.minCredits());
        period.setMaxCredits(request.maxCredits() == null ? 25 : request.maxCredits());
        period.setAllowRetake(request.allowRetake() == null ? true : request.allowRetake());
        if (period.getIsOpen() == null) {
            period.setIsOpen(request.isOpen() == null ? true : request.isOpen());
        } else if (request.isOpen() != null) {
            period.setIsOpen(request.isOpen());
        }
        if (period.getIsActive() == null) {
            period.setIsActive(true);
        }
    }

    private RegistrationPeriod findActive(UUID id) {
        return periodRepository.findByIdAndDeletedAtIsNull(id)
                .filter(period -> Boolean.TRUE.equals(period.getIsActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt đăng ký."));
    }

    private RegistrationPeriodResponse toResponse(RegistrationPeriod period) {
        String semesterName = semesterRepository.findById(period.getSemesterId())
                .map(semester -> semester.getName())
                .orElse(null);
        return new RegistrationPeriodResponse(
                period.getId(),
                period.getName(),
                period.getSemesterId(),
                semesterName,
                period.getStartTime(),
                period.getEndTime(),
                period.getMinCredits(),
                period.getMaxCredits(),
                period.getAllowRetake(),
                period.getIsOpen(),
                period.getIsActive());
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phân trang yêu cầu page >= 0 và size từ 1 đến 100.");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
