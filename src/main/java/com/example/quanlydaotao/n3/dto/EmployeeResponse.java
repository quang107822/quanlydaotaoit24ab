package com.example.quanlydaotao.n3.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public class EmployeeResponse {
    private UUID id;
    private String code;
    @JsonProperty("full_name")
    private String fullName;
    private String email;
    @JsonProperty("is_active")
    private Boolean isActive;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public EmployeeResponse(UUID id, String code, String fullName, String email, Boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.code = code;
        this.fullName = fullName;
        this.email = email;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
