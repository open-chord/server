package com.openchord.server.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class AuthService {
    private static final Duration ACCESS_TTL = Duration.ofMinutes(20);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private final UserRepository users;
    private final UserSessionRepository sessions;
    private final ServerSettingsRepository settings;
    private final PasswordEncoder passwords;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, UserSessionRepository sessions, ServerSettingsRepository settings, PasswordEncoder passwords) {
        this.users = users;
        this.sessions = sessions;
        this.settings = settings;
        this.passwords = passwords;
    }

    public ServerStatus status() {
        return settings.findById(true)
                .map(value -> new ServerStatus(true, value.getMode(), value.isRegistrationEnabled()))
                .orElseGet(() -> new ServerStatus(false, null, false));
    }

    @Transactional
    public AuthResponse setup(Credentials input, ServerMode mode) {
        if (settings.existsById(true) || users.count() > 0) {
            throw new ResponseStatusException(CONFLICT, "Server is already configured");
        }
        Instant now = Instant.now();
        OpenChordUser owner = users.save(new OpenChordUser(
                username(input.username()), displayName(input.displayName()), passwords.encode(password(input.password())), UserRole.OWNER, now));
        settings.save(new ServerSettings(mode, mode == ServerMode.FAMILY, now));
        return issue(owner, input.deviceName(), now);
    }

    @Transactional
    public AuthResponse register(Credentials input) {
        ServerSettings server = settings.findById(true)
                .orElseThrow(() -> new ResponseStatusException(CONFLICT, "Server setup is required"));
        if (server.getMode() != ServerMode.FAMILY || !server.isRegistrationEnabled()) {
            throw new ResponseStatusException(FORBIDDEN, "Registration is closed");
        }
        String username = username(input.username());
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(CONFLICT, "Username is already in use");
        }
        Instant now = Instant.now();
        OpenChordUser member = users.save(new OpenChordUser(
                username, displayName(input.displayName()), passwords.encode(password(input.password())), UserRole.MEMBER, now));
        return issue(member, input.deviceName(), now);
    }

    @Transactional
    public AuthResponse login(LoginRequest input) {
        OpenChordUser user = users.findByUsernameIgnoreCase(username(input.username()))
                .filter(value -> value.getDisabledAt() == null)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!passwords.matches(input.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return issue(user, input.deviceName(), Instant.now());
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        Instant now = Instant.now();
        UserSession session = sessions.findByRefreshTokenHash(hash(refreshToken))
                .filter(value -> value.getRevokedAt() == null && value.getRefreshExpiresAt().isAfter(now))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        TokenPair pair = tokens(now);
        session.rotate(hash(pair.access()), hash(pair.refresh()), now.plus(ACCESS_TTL), now.plus(REFRESH_TTL), now);
        return response(session.getUser(), pair, now);
    }

    @Transactional(readOnly = true)
    public OpenChordUser authenticate(String accessToken) {
        Instant now = Instant.now();
        return sessions.findByAccessTokenHash(hash(accessToken))
                .filter(value -> value.getRevokedAt() == null && value.getAccessExpiresAt().isAfter(now))
                .map(UserSession::getUser)
                .filter(value -> value.getDisabledAt() == null)
                .orElse(null);
    }

    @Transactional
    public void logout(String accessToken) {
        sessions.findByAccessTokenHash(hash(accessToken)).ifPresent(value -> value.revoke(Instant.now()));
    }

    private AuthResponse issue(OpenChordUser user, String deviceName, Instant now) {
        TokenPair pair = tokens(now);
        sessions.save(new UserSession(user, hash(pair.access()), hash(pair.refresh()), now.plus(ACCESS_TTL), now.plus(REFRESH_TTL), normalizeDevice(deviceName), now));
        return response(user, pair, now);
    }

    private AuthResponse response(OpenChordUser user, TokenPair pair, Instant now) {
        return new AuthResponse(pair.access(), pair.refresh(), now.plus(ACCESS_TTL), UserView.from(user));
    }

    private TokenPair tokens(Instant ignored) {
        byte[] access = new byte[32];
        byte[] refresh = new byte[48];
        random.nextBytes(access);
        random.nextBytes(refresh);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return new TokenPair(encoder.encodeToString(access), encoder.encodeToString(refresh));
    }

    private static String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String username(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("[a-z0-9][a-z0-9._-]{2,39}")) {
            throw new IllegalArgumentException("Username must contain 3 to 40 latin letters, numbers, dots, dashes, or underscores");
        }
        return normalized;
    }

    private static String displayName(String value) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty() || normalized.length() > 80) throw new IllegalArgumentException("Display name must contain 1 to 80 characters");
        return normalized;
    }

    private static String password(String value) {
        if (value == null || value.length() < 10 || value.length() > 200) throw new IllegalArgumentException("Password must contain 10 to 200 characters");
        return value;
    }

    private static String normalizeDevice(String value) {
        String normalized = value == null ? "Unknown device" : value.strip();
        return normalized.isEmpty() ? "Unknown device" : normalized.substring(0, Math.min(normalized.length(), 120));
    }

    private record TokenPair(String access, String refresh) {}
    public record ServerStatus(boolean initialized, ServerMode mode, boolean registrationEnabled) {}
    public record Credentials(String username, String displayName, String password, String deviceName) {}
    public record LoginRequest(String username, String password, String deviceName) {}
    public record RefreshRequest(String refreshToken) {}
    public record SetupRequest(String username, String displayName, String password, String deviceName, ServerMode mode) {
        Credentials credentials() { return new Credentials(username, displayName, password, deviceName); }
    }
    public record AuthResponse(String accessToken, String refreshToken, Instant accessExpiresAt, UserView user) {}
    public record UserView(java.util.UUID id, String username, String displayName, UserRole role) {
        static UserView from(OpenChordUser user) { return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole()); }
    }
}
