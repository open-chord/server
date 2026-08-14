package com.openchord.server.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "user_id") private OpenChordUser user;
    private String accessTokenHash;
    private String refreshTokenHash;
    private Instant accessExpiresAt;
    private Instant refreshExpiresAt;
    private String deviceName;
    private Instant createdAt;
    private Instant lastUsedAt;
    private Instant revokedAt;

    protected UserSession() {}

    public UserSession(OpenChordUser user, String accessHash, String refreshHash, Instant accessExpiry, Instant refreshExpiry, String deviceName, Instant now) {
        this.user = user;
        this.accessTokenHash = accessHash;
        this.refreshTokenHash = refreshHash;
        this.accessExpiresAt = accessExpiry;
        this.refreshExpiresAt = refreshExpiry;
        this.deviceName = deviceName;
        this.createdAt = now;
        this.lastUsedAt = now;
    }

    public UUID getId() { return id; }
    public OpenChordUser getUser() { return user; }
    public String getAccessTokenHash() { return accessTokenHash; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public Instant getAccessExpiresAt() { return accessExpiresAt; }
    public Instant getRefreshExpiresAt() { return refreshExpiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void rotate(String accessHash, String refreshHash, Instant accessExpiry, Instant refreshExpiry, Instant now) {
        accessTokenHash = accessHash;
        refreshTokenHash = refreshHash;
        accessExpiresAt = accessExpiry;
        refreshExpiresAt = refreshExpiry;
        lastUsedAt = now;
    }
    public void revoke(Instant now) { revokedAt = now; }
}
