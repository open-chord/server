package com.openchord.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Optional shared-secret boundary for administration endpoints. */
@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-OpenChord-Admin-Key";

    private final byte[] expected;

    public AdminApiKeyFilter(OpenChordProperties properties) {
        expected = properties.adminApiKey().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return expected.length == 0 || !request.getRequestURI().startsWith("/api/admin/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        byte[] supplied = header == null ? new byte[0] : header.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, supplied)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "OpenChordAdminKey");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Invalid or missing administration API key\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
