package com.example.quanlydaotao.n7.service;

import com.example.quanlydaotao.n7.entity.Schedule;
import com.example.quanlydaotao.n7.repository.ScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ScheduleService {

    private final ScheduleRepository repository;

    public ScheduleService(ScheduleRepository repository) {
        this.repository = repository;
    }

    // 1. Danh sách và tìm kiếm lịch học
    @Transactional(readOnly = true)
    public List<Schedule> search(
            UUID courseSectionId,
            UUID employeeId,
            UUID roomId,
            Integer dayOfWeek,
            String status,
            Boolean isActive) {

        return repository.search(
                courseSectionId,
                employeeId,
                roomId,
                dayOfWeek,
                status,
                isActive);
    }

    // 2. Chi tiết lịch học
    @Transactional(readOnly = true)
    public Schedule getById(UUID id) {
        return repository.findActiveById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy lịch học: " + id));
    }

    // 3. Thêm lịch học
    public Schedule create(Schedule schedule) {
        validateSchedule(schedule);

        LocalDateTime now = LocalDateTime.now();

        schedule.setId(UUID.randomUUID());
        schedule.setCreatedAt(now);
        schedule.setUpdatedAt(now);
        schedule.setDeletedAt(null);
        schedule.setDeletedBy(null);

        if (schedule.getIsActive() == null) {
            schedule.setIsActive(true);
        }

        return repository.save(schedule);
    }

    // 4. Cập nhật lịch học
    public Schedule update(UUID id, Schedule request) {
        Schedule current = getById(id);

        validateSchedule(request);

        current.setCourseSectionId(request.getCourseSectionId());
        current.setEmployeeId(request.getEmployeeId());
        current.setRoomId(request.getRoomId());
        current.setDayOfWeek(request.getDayOfWeek());
        current.setDate(request.getDate());
        current.setShiftId(request.getShiftId());
        current.setStartPeriod(request.getStartPeriod());
        current.setEndPeriod(request.getEndPeriod());
        current.setNumberOfPeriods(request.getNumberOfPeriods());
        current.setStartDate(request.getStartDate());
        current.setEndDate(request.getEndDate());
        current.setMode(request.getMode());
        current.setStatus(request.getStatus());
        current.setDescription(request.getDescription());
        current.setScheduleStatus(request.getScheduleStatus());
        current.setNote(request.getNote());
        current.setIsActive(request.getIsActive());
        current.setUpdatedBy(request.getUpdatedBy());
        current.setUpdatedAt(LocalDateTime.now());

        return repository.save(current);
    }

    // 5. Xóa mềm lịch học
    public void delete(UUID id, UUID deletedBy) {
        Schedule schedule = getById(id);

        LocalDateTime now = LocalDateTime.now();

        schedule.setDeletedAt(now);
        schedule.setDeletedBy(deletedBy);
        schedule.setUpdatedAt(now);
        schedule.setIsActive(false);

        repository.save(schedule);
    }

    // 6. Cập nhật trạng thái lịch học
    public Schedule updateStatus(UUID id, String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái không được để trống");
        }

        Schedule schedule = getById(id);
        schedule.setStatus(status);
        schedule.setUpdatedAt(LocalDateTime.now());

        return repository.save(schedule);
    }

    private void validateSchedule(Schedule schedule) {
        if (schedule.getStartPeriod() != null
                && schedule.getEndPeriod() != null
                && schedule.getStartPeriod() > schedule.getEndPeriod()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tiết bắt đầu phải nhỏ hơn hoặc bằng tiết kết thúc");
        }

        if (schedule.getNumberOfPeriods() != null
                && schedule.getNumberOfPeriods() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tiết học phải lớn hơn 0");
        }

        if (schedule.getStartDate() != null
                && schedule.getEndDate() != null
                && schedule.getStartDate()
                        .isAfter(schedule.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thời gian bắt đầu không được sau thời gian kết thúc");
        }
    }
}