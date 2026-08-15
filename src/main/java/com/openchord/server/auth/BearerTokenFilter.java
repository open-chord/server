package com.openchord.server.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenFilter extends OncePerRequestFilter {
    private final AuthService auth;

    public BearerTokenFilter(AuthService auth) {
        this.auth = auth;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = token(request);
        if (token != null) {
            OpenChordUser user = auth.authenticate(token);
            if (user != null) {
                SecurityContextHolder.getContext()
                        .setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        token,
                                        java.util.List.of(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_" + user.getRole().name()))));
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Extracts an access token without widening query-token support to JSON APIs.
     *
     * <p>AVPlayer media requests cannot reliably attach the bearer header, so only paths below
     * {@code /media/} accept the equivalent {@code access_token} query parameter.
     */
    static String token(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).strip();
        }
        return request.getRequestURI().startsWith("/media/")
                ? request.getParameter("access_token")
                : null;
    }
}
