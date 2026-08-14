package com.openchord.server.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerSettingsRepository extends JpaRepository<ServerSettings, Boolean> {}
