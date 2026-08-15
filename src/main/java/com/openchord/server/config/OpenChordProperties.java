package com.openchord.server.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Filesystem and public URL settings for managed OpenChord media.
 *
 * @param mediaRoot     root below which audio, artwork, and temporary imports are stored
 * @param publicBaseUrl externally reachable server URL used to construct media links
 * @param lyricsAlignerUrl optional URL of a local lyrics-alignment sidecar
 */
@ConfigurationProperties(prefix = "openchord")
public record OpenChordProperties(Path mediaRoot, String publicBaseUrl, String lyricsAlignerUrl) {
    public OpenChordProperties {
        publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
        lyricsAlignerUrl = lyricsAlignerUrl == null ? "" : lyricsAlignerUrl.replaceAll("/+$", "");
    }
}
