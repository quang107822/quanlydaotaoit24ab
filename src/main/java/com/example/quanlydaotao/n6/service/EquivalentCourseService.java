package com.example.quanlydaotao.n6.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.quanlydaotao.n6.dto.RegistrationDtos.EquivalentCourseRequest;
import com.example.quanlydaotao.n6.dto.RegistrationDtos.EquivalentCourseResponse;
import com.example.quanlydaotao.n6.entity.CourseLookup;
import com.example.quanlydaotao.n6.entity.EquivalentCourse;
import com.example.quanlydaotao.n6.repository.CourseLookupRepository;
import com.example.quanlydaotao.n6.repository.EquivalentCourseRepository;

@Service
public class EquivalentCourseService {

    private final EquivalentCourseRepository repository;
    private final CourseLookupRepository courseRepository;

    public EquivalentCourseService(
            EquivalentCourseRepository repository,
            CourseLookupRepository courseRepository) {
        this.repository = repository;
        this.courseRepository = courseRepository;
    }

    @Transactional(readOnly = true)
    public List<EquivalentCourseResponse> list() {
        return repository.findByIsActiveTrueAndDeletedAtIsNullOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EquivalentCourseResponse create(EquivalentCourseRequest request) {
        validate(request, null);
        EquivalentCourse equivalentCourse = new EquivalentCourse();
        apply(equivalentCourse, request);
        return toResponse(repository.save(equivalentCourse));
    }

    @Transactional
    public EquivalentCourseResponse update(UUID id, EquivalentCourseRequest request) {
        EquivalentCourse equivalentCourse = findActive(id);
        validate(request, id);
        apply(equivalentCourse, request);
        return toResponse(repository.save(equivalentCourse));
    }

    @Transactional
    public void delete(UUID id) {
        EquivalentCourse equivalentCourse = findActive(id);
        equivalentCourse.setIsActive(false);
        equivalentCourse.setDeletedAt(LocalDateTime.now());
        repository.save(equivalentCourse);
    }

    private void validate(EquivalentCourseRequest request, UUID excludeId) {
        if (request.originalCourseId().equals(request.equivalentCourseId())) {
            throw badRequest("Môn gốc và môn thay thế phải khác nhau.");
        }
        findCourse(request.originalCourseId());
        findCourse(request.equivalentCourseId());
        boolean duplicate = excludeId == null
                ? repository.existsByOriginalCourseIdAndEquivalentCourseIdAndIsActiveTrueAndDeletedAtIsNull(
                        request.originalCourseId(), request.equivalentCourseId())
                : repository.existsByOriginalCourseIdAndEquivalentCourseIdAndIsActiveTrueAndDeletedAtIsNullAndIdNot(
                        request.originalCourseId(), request.equivalentCourseId(), excludeId);
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quy tắc tương đương này đã tồn tại.");
        }
    }

    private void apply(EquivalentCourse target, EquivalentCourseRequest request) {
        target.setOriginalCourseId(request.originalCourseId());
        target.setEquivalentCourseId(request.equivalentCourseId());
        target.setEquivalenceType(request.equivalenceType());
        target.setEffectDate(request.effectDate());
        target.setNote(request.note());
        target.setIsActive(true);
        target.setDeletedAt(null);
    }

    private EquivalentCourseResponse toResponse(EquivalentCourse relation) {
        CourseLookup original = findCourse(relation.getOriginalCourseId());
        CourseLookup equivalent = findCourse(relation.getEquivalentCourseId());
        return new EquivalentCourseResponse(
                relation.getId(),
                relation.getOriginalCourseId(),
                original.getCode(),
                original.getName(),
                relation.getEquivalentCourseId(),
                equivalent.getCode(),
                equivalent.getName(),
                relation.getEquivalenceType(),
                relation.getEffectDate(),
                relation.getNote());
    }

    private EquivalentCourse findActive(UUID id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .filter(EquivalentCourse::getIsActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy quy tắc tương đương."));
    }

    private CourseLookup findCourse(UUID id) {
        return courseRepository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy môn học."));
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
