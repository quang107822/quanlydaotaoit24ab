package com.example.quanlydaotao.n7.controller;

import com.example.quanlydaotao.n7.entity.Schedule;
import com.example.quanlydaotao.n7.service.ScheduleService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // GET: Danh sách và tìm kiếm lịch học
    @GetMapping
    public ResponseEntity<List<Schedule>> getAll(
            @RequestParam(required = false) UUID courseSectionId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) Integer dayOfWeek,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive) {

        return ResponseEntity.ok(
                scheduleService.search(
                        courseSectionId,
                        employeeId,
                        roomId,
                        dayOfWeek,
                        status,
                        isActive));
    }

    // GET: Chi tiết lịch học
    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                scheduleService.getById(id));
    }

    // POST: Thêm lịch học
    @PostMapping
    public ResponseEntity<Schedule> create(
            @RequestBody Schedule schedule) {

        Schedule created = scheduleService.create(schedule);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    // PUT: Cập nhật lịch học
    @PutMapping("/{id}")
    public ResponseEntity<Schedule> update(
            @PathVariable UUID id,
            @RequestBody Schedule schedule) {

        return ResponseEntity.ok(
                scheduleService.update(id, schedule));
    }

    // DELETE: Xóa mềm lịch học
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID deletedBy) {

        scheduleService.delete(id, deletedBy);

        return ResponseEntity.noContent().build();
    }

    // PATCH: Cập nhật trạng thái
    @PatchMapping("/{id}/status")
    public ResponseEntity<Schedule> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        return ResponseEntity.ok(
                scheduleService.updateStatus(
                        id, body.get("status")));
    }
}
