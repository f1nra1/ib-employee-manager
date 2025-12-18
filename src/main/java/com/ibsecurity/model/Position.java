package com.ibsecurity.model;

import java.time.LocalDateTime;

public class Position {
    private int id;
    private String title;
    private String description;
    private int securityLevel;
    private LocalDateTime createdAt;

    public Position() {}

    public Position(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getSecurityLevel() { return securityLevel; }
    public void setSecurityLevel(int securityLevel) { this.securityLevel = securityLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return title;
    }
}
