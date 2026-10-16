package com.example.quanlydaotao.n3.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public class EmployeePositionResponse {
    private UUID id;
    @JsonProperty("employee_id")
    private UUID employeeId;
    @JsonProperty("position_id")
    private UUID positionId;
    private String description;
    @JsonProperty("start_date")
    private LocalDateTime startDate;
    @JsonProperty("end_date")
    private LocalDateTime endDate;
    @JsonProperty("is_active")
    private Boolean isActive;

    public EmployeePositionResponse(UUID id, UUID employeeId, UUID positionId, String description, LocalDateTime startDate, LocalDateTime endDate, Boolean isActive) {
        this.id = id;
        this.employeeId = employeeId;
        this.positionId = positionId;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public UUID getPositionId() { return positionId; }
    public String getDescription() { return description; }
    public LocalDateTime getStartDate() { return startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public Boolean getIsActive() { return isActive; }
}
