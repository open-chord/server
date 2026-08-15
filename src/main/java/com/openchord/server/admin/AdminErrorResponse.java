package com.openchord.server.admin;

/** Stable error body returned for rejected administration requests. */
public record AdminErrorResponse(String message) {
}
