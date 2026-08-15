package com.openchord.server.auth;

import com.openchord.server.auth.AuthService.AuthResponse;
import com.openchord.server.auth.AuthService.Credentials;
import com.openchord.server.auth.AuthService.LoginRequest;
import com.openchord.server.auth.AuthService.RefreshRequest;
import com.openchord.server.auth.AuthService.ServerStatus;
import com.openchord.server.auth.AuthService.SetupRequest;
import com.openchord.server.auth.AuthService.UserView;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session API used to bootstrap a server and issue opaque access and refresh tokens.
 *
 * <p>Only setup, registration, login, refresh, and server discovery are public. Identity lookup
 * and logout require an access token through {@link BearerTokenFilter}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/server")
    public ServerStatus server() {
        return auth.status();
    }

    @PostMapping("/setup")
    public AuthResponse setup(@RequestBody SetupRequest request) {
        return auth.setup(request.credentials(), request.mode());
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody Credentials request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        return auth.refresh(request.refreshToken());
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal OpenChordUser user) {
        return UserView.from(user);
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        String token = BearerTokenFilter.token(request);
        if (token != null) auth.logout(token);
    }
}
