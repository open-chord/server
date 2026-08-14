package com.openchord.server.auth;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByAccessTokenHash(String hash);
    Optional<UserSession> findByRefreshTokenHash(String hash);
}
