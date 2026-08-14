package com.openchord.server.playback;

import com.openchord.server.catalog.Track;
import com.openchord.server.auth.OpenChordUser;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record of client-reported playback state at a point in time.
 *
 * <p>Events are append-only and drive album recency. The service layer validates the track and
 * normalizes the position before constructing this entity.
 */
@Entity
@Table(name = "playback_events")
public class PlaybackEvent {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id")
    private Track track;

    private Instant playedAt;
    private long positionMs;
    private boolean completed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private OpenChordUser user;

    protected PlaybackEvent() {
    }

    public PlaybackEvent(Track track, Instant playedAt, long positionMs, boolean completed) {
        this(track, playedAt, positionMs, completed, null);
    }

    public PlaybackEvent(Track track, Instant playedAt, long positionMs, boolean completed, OpenChordUser user) {
        this.track = track;
        this.playedAt = playedAt;
        this.positionMs = positionMs;
        this.completed = completed;
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public Track getTrack() {
        return track;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public long getPositionMs() {
        return positionMs;
    }

    public boolean isCompleted() {
        return completed;
    }

    public OpenChordUser getUser() { return user; }
}
