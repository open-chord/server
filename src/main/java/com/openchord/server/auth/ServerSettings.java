package com.openchord.server.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "server_settings")
public class ServerSettings {
    @Id private Boolean id = true;
    @Enumerated(EnumType.STRING) private ServerMode mode;
    private boolean registrationEnabled;
    private Instant initializedAt;

    protected ServerSettings() {}

    public ServerSettings(ServerMode mode, boolean registrationEnabled, Instant initializedAt) {
        this.mode = mode;
        this.registrationEnabled = registrationEnabled;
        this.initializedAt = initializedAt;
    }

    public ServerMode getMode() { return mode; }
    public boolean isRegistrationEnabled() { return registrationEnabled; }
    public Instant getInitializedAt() { return initializedAt; }
    public void setRegistrationEnabled(boolean enabled) { registrationEnabled = enabled; }
}
