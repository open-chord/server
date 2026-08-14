package com.openchord.server.graphql;

import com.openchord.server.catalog.CatalogService;
import com.openchord.server.auth.OpenChordUser;
import com.openchord.server.config.OpenChordProperties;
import com.openchord.server.graphql.CatalogTypes.AlbumView;
import com.openchord.server.graphql.CatalogTypes.PlaybackEventInput;
import com.openchord.server.graphql.CatalogTypes.PlaybackEventView;
import com.openchord.server.graphql.CatalogTypes.PlaylistView;
import com.openchord.server.playback.PlaybackService;
import com.openchord.server.playlist.PlaylistNotFoundException;
import com.openchord.server.playlist.PlaylistService;

import java.util.List;
import java.util.UUID;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Resolves the public catalog queries and playback mutation defined in {@code schema.graphqls}.
 *
 * <p>Domain entities never cross the GraphQL boundary; they are mapped to immutable transport
 * records and media paths are expanded using the configured public base URL.
 */
@Controller
public class CatalogGraphQlController {
    private final CatalogService catalog;
    private final PlaybackService playback;
    private final PlaylistService playlists;
    private final OpenChordProperties properties;

    public CatalogGraphQlController(
            CatalogService catalog,
            PlaybackService playback,
            PlaylistService playlists,
            OpenChordProperties properties) {
        this.catalog = catalog;
        this.playback = playback;
        this.playlists = playlists;
        this.properties = properties;
    }

    @QueryMapping
    public List<AlbumView> albums(
            @Argument String search, @Argument Integer limit, @Argument Integer offset) {
        return catalog.albums(search, limit == null ? 50 : limit, offset == null ? 0 : offset).stream()
                .map(album -> AlbumView.from(album, properties))
                .toList();
    }

    @QueryMapping
    public AlbumView album(@Argument UUID id) {
        return catalog.album(id).map(value -> AlbumView.from(value, properties)).orElse(null);
    }

    @QueryMapping
    public List<AlbumView> recentlyPlayed(@AuthenticationPrincipal OpenChordUser user, @Argument Integer limit) {
        return catalog.recentlyPlayed(user, limit == null ? 10 : limit).stream()
                .map(album -> AlbumView.from(album, properties))
                .toList();
    }

    @QueryMapping
    public List<PlaylistView> playlists(@AuthenticationPrincipal OpenChordUser user) {
        return playlists.playlists(user).stream()
                .map(playlist -> PlaylistView.from(playlist, properties))
                .toList();
    }

    @QueryMapping
    public PlaylistView playlist(@AuthenticationPrincipal OpenChordUser user, @Argument UUID id) {
        try {
            return PlaylistView.from(playlists.playlist(user, id), properties);
        } catch (PlaylistNotFoundException ignored) {
            return null;
        }
    }

    @MutationMapping
    public PlaybackEventView recordPlayback(@AuthenticationPrincipal OpenChordUser user, @Argument PlaybackEventInput input) {
        return PlaybackEventView.from(playback.record(user, input));
    }

    @MutationMapping
    public PlaylistView createPlaylist(@AuthenticationPrincipal OpenChordUser user, @Argument String name) {
        return PlaylistView.from(playlists.create(user, name), properties);
    }

    @MutationMapping
    public PlaylistView renamePlaylist(@AuthenticationPrincipal OpenChordUser user, @Argument UUID id, @Argument String name) {
        return PlaylistView.from(playlists.rename(user, id, name), properties);
    }

    @MutationMapping
    public boolean deletePlaylist(@AuthenticationPrincipal OpenChordUser user, @Argument UUID id) {
        return playlists.delete(user, id);
    }

    @MutationMapping
    public PlaylistView addTrackToPlaylist(
            @AuthenticationPrincipal OpenChordUser user, @Argument UUID playlistId, @Argument UUID trackId) {
        return PlaylistView.from(playlists.addTrack(user, playlistId, trackId), properties);
    }

    @MutationMapping
    public PlaylistView removeTrackFromPlaylist(
            @AuthenticationPrincipal OpenChordUser user, @Argument UUID playlistId, @Argument UUID trackId) {
        return PlaylistView.from(playlists.removeTrack(user, playlistId, trackId), properties);
    }

    @MutationMapping
    public PlaylistView moveTrackInPlaylist(
            @AuthenticationPrincipal OpenChordUser user, @Argument UUID playlistId, @Argument UUID trackId, @Argument int position) {
        return PlaylistView.from(playlists.moveTrack(user, playlistId, trackId, position), properties);
    }
}
