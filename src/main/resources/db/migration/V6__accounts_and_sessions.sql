CREATE TABLE server_settings
(
    id                    BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (id),
    mode                  VARCHAR(16)              NOT NULL CHECK (mode IN ('PERSONAL', 'FAMILY')),
    registration_enabled  BOOLEAN                  NOT NULL,
    initialized_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE users
(
    id            UUID PRIMARY KEY,
    username      VARCHAR(40)              NOT NULL,
    display_name  VARCHAR(80)              NOT NULL,
    password_hash VARCHAR(255)             NOT NULL,
    role          VARCHAR(16)              NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    disabled_at   TIMESTAMP WITH TIME ZONE
);
CREATE UNIQUE INDEX ux_users_username_ci ON users (lower(username));

CREATE TABLE user_sessions
(
    id                 UUID PRIMARY KEY,
    user_id            UUID                     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    access_token_hash  VARCHAR(64)              NOT NULL UNIQUE,
    refresh_token_hash VARCHAR(64)              NOT NULL UNIQUE,
    access_expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    device_name        VARCHAR(120)             NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at         TIMESTAMP WITH TIME ZONE
);
CREATE INDEX ix_user_sessions_user ON user_sessions (user_id);
CREATE INDEX ix_user_sessions_access ON user_sessions (access_token_hash);

ALTER TABLE playlists ADD COLUMN owner_id UUID REFERENCES users (id) ON DELETE CASCADE;
CREATE INDEX ix_playlists_owner_updated ON playlists (owner_id, updated_at DESC);

ALTER TABLE playback_events ADD COLUMN user_id UUID REFERENCES users (id) ON DELETE CASCADE;
CREATE INDEX ix_playback_events_user_played ON playback_events (user_id, played_at DESC);
