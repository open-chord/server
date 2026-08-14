package com.openchord.server.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class OpenChordUser {
    @Id @GeneratedValue private UUID id;
    private String username;
    private String displayName;
    private String passwordHash;
    @Enumerated(EnumType.STRING) private UserRole role;
    private Instant createdAt;
    private Instant disabledAt;

    protected OpenChordUser() {}

    public OpenChordUser(String username, String displayName, String passwordHash, UserRole role, Instant createdAt) {
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDisabledAt() { return disabledAt; }
}
