package com.example.quanlydaotao.n7.repository;

import com.example.quanlydaotao.n7.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository
        extends JpaRepository<Schedule, UUID> {

    @Query("""
            SELECT s FROM Schedule s
            WHERE s.deletedAt IS NULL
              AND (:courseSectionId IS NULL
                   OR s.courseSectionId = :courseSectionId)
              AND (:employeeId IS NULL
                   OR s.employeeId = :employeeId)
              AND (:roomId IS NULL
                   OR s.roomId = :roomId)
              AND (:dayOfWeek IS NULL
                   OR s.dayOfWeek = :dayOfWeek)
              AND (:status IS NULL
                   OR s.status = :status)
              AND (:isActive IS NULL
                   OR s.isActive = :isActive)
            ORDER BY s.startDate ASC
            """)
    List<Schedule> search(
            @Param("courseSectionId") UUID courseSectionId,
            @Param("employeeId") UUID employeeId,
            @Param("roomId") UUID roomId,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("status") String status,
            @Param("isActive") Boolean isActive);

    @Query("""
            SELECT s FROM Schedule s
            WHERE s.id = :id AND s.deletedAt IS NULL
            """)
    Optional<Schedule> findActiveById(
            @Param("id") UUID id);
}