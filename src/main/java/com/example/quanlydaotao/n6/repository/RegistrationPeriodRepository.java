package com.example.quanlydaotao.n6.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.quanlydaotao.n6.entity.RegistrationPeriod;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, UUID> {

    @Query("""
            select p from RegistrationPeriod p
            where p.deletedAt is null
              and (:semesterId is null or p.semesterId = :semesterId)
              and (:isActive is null or p.isActive = :isActive)
              and (:isOpen is null or p.isOpen = :isOpen)
              and (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
            """)
    Page<RegistrationPeriod> search(
            @Param("semesterId") UUID semesterId,
            @Param("isActive") Boolean isActive,
            @Param("isOpen") Boolean isOpen,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<RegistrationPeriod> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select count(p) > 0 from RegistrationPeriod p
            where p.semesterId = :semesterId
              and p.deletedAt is null
              and p.startTime < :endTime
              and p.endTime > :startTime
              and (:excludeId is null or p.id <> :excludeId)
            """)
    boolean overlaps(
            @Param("semesterId") UUID semesterId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") UUID excludeId);
}
