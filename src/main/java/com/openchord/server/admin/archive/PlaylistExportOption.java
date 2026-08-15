package com.openchord.server.admin.archive;

import java.util.UUID;

/** Playlist choice displayed by portable archive export clients. */
public record PlaylistExportOption(UUID id, String name, int tracks) {
}
