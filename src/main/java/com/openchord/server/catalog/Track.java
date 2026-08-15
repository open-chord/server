package com.openchord.server.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Playable catalog track and owner of its synchronized lyric lines.
 *
 * <p>Audio paths are relative to the managed media root. Relationship helpers keep the in-memory
 * aggregate consistent with the owning JPA associations.
 */
@Entity
@Table(name = "tracks")
public class Track {
    @Id
    @GeneratedValue
    private UUID id;
    private String title;
    private long durationMs;
    private int discNumber;
    private int number;
    private String audioPath;
    private String contentType;
    private String originalPath;
    private String originalContentType;
    private String originalFilename;

    @Column(columnDefinition = "TEXT")
    private String lyricsSource;

    @Enumerated(EnumType.STRING)
    private LyricsStatus lyricsStatus = LyricsStatus.EMPTY;
    private String lyricsAlignmentEngine;

    @Column(columnDefinition = "TEXT")
    private String lyricsAlignmentError;

    private Float lyricsAverageConfidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id")
    private Album album;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startMs")
    private Set<LyricLine> lyrics = new LinkedHashSet<>();

    protected Track() {
    }

    public Track(
            String title,
            long durationMs,
            int discNumber,
            int number,
            String audioPath,
            String contentType) {
        this.title = title;
        this.durationMs = durationMs;
        this.discNumber = discNumber;
        this.number = number;
        this.audioPath = audioPath;
        this.contentType = contentType;
        this.originalPath = audioPath;
        this.originalContentType = contentType;
        this.originalFilename = Path.of(audioPath).getFileName().toString();
    }

    public void setOriginalMedia(String path, String contentType, String filename) {
        this.originalPath = path;
        this.originalContentType = contentType;
        this.originalFilename = filename;
    }

    void attachTo(Album album) {
        this.album = album;
    }

    public void addLyricLine(LyricLine line) {
        lyrics.add(line);
        line.attachTo(this);
        lyricsStatus = LyricsStatus.SYNCED;
    }

    /**
     * Replaces the lyric collection while maintaining each line's owning relationship.
     *
     * @param lines new synchronized lyrics in playback order
     */
    public void replaceLyrics(List<LyricLine> lines) {
        lyrics.clear();
        lines.forEach(this::addLyricLine);
        lyricsStatus = lines.isEmpty() ? sourceStatus() : LyricsStatus.SYNCED;
    }

    /** Stores editable source text and invalidates timings derived from an older source. */
    public void replaceLyricsSource(String source) {
        lyricsSource = normalizeSource(source);
        lyrics.clear();
        lyricsStatus = sourceStatus();
        clearAlignmentMetadata();
    }

    /** Stores both the source document and synchronized lines from an LRC import. */
    public void replaceSynchronizedLyrics(String source, List<LyricLine> lines) {
        lyricsSource = normalizeSource(source);
        clearAlignmentMetadata();
        replaceLyrics(lines);
    }

    public void beginLyricsAlignment() {
        if (lyricsSource == null) throw new IllegalStateException("Lyrics source is empty");
        lyricsStatus = LyricsStatus.PROCESSING;
        lyricsAlignmentError = null;
    }

    public void completeLyricsAlignment(List<LyricLine> lines, String engine, float confidence) {
        if (lines.isEmpty()) throw new IllegalArgumentException("Alignment returned no lyric lines");
        replaceLyrics(lines);
        lyricsStatus = LyricsStatus.NEEDS_REVIEW;
        lyricsAlignmentEngine = engine;
        lyricsAverageConfidence = confidence;
        lyricsAlignmentError = null;
    }

    public void failLyricsAlignment(String error) {
        lyrics.clear();
        lyricsStatus = LyricsStatus.FAILED;
        lyricsAlignmentError = error == null || error.isBlank() ? "Lyrics alignment failed" : error;
        lyricsAverageConfidence = null;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getDiscNumber() {
        return discNumber;
    }

    public int getNumber() {
        return number;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public String getContentType() {
        return contentType;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public String getOriginalContentType() {
        return originalContentType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public Album getAlbum() {
        return album;
    }

    /**
     * Returns synchronized lyrics in timestamp order.
     *
     * @return immutable ordered snapshot of the lyric collection
     */
    public List<LyricLine> getLyrics() {
        return List.copyOf(lyrics);
    }

    public String getLyricsSource() {
        return lyricsSource == null ? "" : lyricsSource;
    }

    public LyricsStatus getLyricsStatus() {
        return lyricsStatus;
    }

    public String getLyricsAlignmentEngine() {
        return lyricsAlignmentEngine;
    }

    public String getLyricsAlignmentError() {
        return lyricsAlignmentError;
    }

    public Float getLyricsAverageConfidence() {
        return lyricsAverageConfidence;
    }

    private LyricsStatus sourceStatus() {
        return lyricsSource == null ? LyricsStatus.EMPTY : LyricsStatus.UNSYNCED;
    }

    private static String normalizeSource(String source) {
        if (source == null || source.isBlank()) return null;
        return source.replace("\r", "").strip();
    }

    private void clearAlignmentMetadata() {
        lyricsAlignmentEngine = null;
        lyricsAlignmentError = null;
        lyricsAverageConfidence = null;
    }
}
