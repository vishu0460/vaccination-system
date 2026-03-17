package com.vaccine.common.dto;

import java.time.LocalDateTime;

public class FeedbackResponse {
    private Long id;
    private String userEmail;
    private Integer rating;
    private String subject;
    private String message;
    private String status;
    private String adminResponse;
    private LocalDateTime createdAt;

    // Constructors
    public FeedbackResponse() {}

    public FeedbackResponse(Long id, String userEmail, Integer rating, String subject, String message, String status, String adminResponse, LocalDateTime createdAt) {
        this.id = id;
        this.userEmail = userEmail;
        this.rating = rating;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.adminResponse = adminResponse;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
