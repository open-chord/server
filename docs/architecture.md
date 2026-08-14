# Backend architecture

This document describes the contracts that are not obvious from the HTTP and
GraphQL schemas: ownership, transaction boundaries, filesystem behavior and
operational assumptions.

## Module boundaries

| Package | Responsibility |
| --- | --- |
| `catalog` | JPA catalog aggregate, catalog queries and repository fetch plans |
| `playlist` | Ordered playlists, artwork and track membership APIs |
| `graphql` | Public catalog and playback API mapping |
| `playback` | Append-only playback events and recently played ordering |
| `media` | Validated filesystem lookup and HTTP delivery of audio and artwork |
| `admin` | Trusted-network mutation, album import and `.openchord` archive workflows |
| `config` | GraphQL scalar wiring and typed `openchord.*` configuration |

Controllers translate transport values into application calls. Services own
validation and transaction boundaries. Repositories define fetch plans needed
while mapping lazy JPA associations.

## Catalog aggregate

`Album` is the persistence aggregate root for tracks:

- an album belongs to one artist;
- an album owns its tracks through cascade and orphan removal;
- a track owns its synchronized lyric lines through cascade and orphan removal;
- `Album.addTrack`, `Track.addLyricLine`, and `Track.replaceLyrics` maintain both
  sides of the in-memory JPA relationships;
- tracks are exposed in disc/track order and lyrics in timestamp order;
- `audioPath` and `artworkPath` are paths relative to `MEDIA_ROOT`, never public
  URLs or arbitrary filesystem paths.

The database migrations are the authoritative source for storage constraints.
The GraphQL schema is the authoritative public read contract.

Playlists are separate ordered aggregates that reference catalog tracks. A
playlist can own managed artwork and is included in a whole-library archive, or
exported by itself together with the albums and media required by its tracks.
The current membership constraint permits one occurrence of a track per
playlist, so repeated occurrences of the same track are not represented yet.

## Read path

GraphQL catalog queries load complete album projections inside read-only
transactions. Mapping to API records must happen before the transaction closes
because artist, track, and lyric relationships are lazy by default. `limit` is
clamped to 1–100 for catalog queries and 1–50 for recently played queries;
negative offsets behave as zero.

Playback events are append-only. `recentlyPlayed` groups events by album and
orders albums by their latest event timestamp. Client positions greater than
the stored duration are clamped. Negative positions and unknown track IDs are
GraphQL errors.

## Media delivery

`MediaController` resolves every stored path against the normalized media root
and rejects paths outside it. Missing database rows, missing files and rejected
paths are all reported as `404` without exposing local paths.

Track delivery supports a complete response or exactly one byte range:

- no `Range` header: `200 OK`;
- one satisfiable range: `206 Partial Content`;
- malformed, multiple, or unsatisfiable ranges: `416 Range Not Satisfiable`.

Audio is private-cacheable for one hour. Artwork is public-cacheable for one
day. Media is served from local storage; horizontal deployments therefore need
a shared media volume or an external object-storage implementation.

## Album import lifecycle

Import is intentionally split into review and commit:

1. `analyze` writes supported uploads below
   `MEDIA_ROOT/.imports/{draftId}`.
2. FFprobe extracts metadata. Filename heuristics fill selected missing values,
   and the response reports warnings for review.
3. The client sends reviewed metadata and the opaque staged filenames to
   `commit`.
4. `commit` resolves every staged filename inside that draft directory, copies
   compatible audio, transcodes FLAC/WAV/AIFF to ALAC, and flushes the album
   aggregate.
5. The staging directory is deleted after a successful database flush. Spring
   commits the surrounding transaction after the service method returns.

Drafts currently have no expiry timestamp or automatic cleanup. An abandoned
analysis therefore leaves its staging directory until an operator removes it.

## Portable archive lifecycle

The admin archive API imports and exports versioned `.openchord` ZIP archives.
Whole-library exports include albums, tracks, playlists, ordering, artwork and
managed playable audio. Playlist exports include the selected playlist and the
dependency closure needed to restore its tracks.

Exports are streamed rather than assembled fully in memory. Imports unpack to
a staging directory and validate entry paths, declared sizes, extracted sizes,
checksums and manifest references before accepting catalog data and media.

Format version `0.1` exports both original and playable renditions when known.
Existing catalog rows are migrated by treating their current managed audio as
both roles; new lossless album imports retain their source bytes separately
from the generated ALAC rendition.

### Consistency boundary

`@Transactional` protects database changes only. File copies, FFmpeg output and
deletions do not participate in the PostgreSQL transaction. Consequently:

- a failed database commit can leave unreferenced artwork or audio;
- a failure during copying or transcoding can leave a partial target file;
- cleanup failure rolls the database transaction back but can leave generated
  media and the staging copy;
- a database commit failure can occur after staging has already been deleted.

Production maintenance should reconcile files under `tracks/` and `artwork/`
against catalog paths. Remove `.imports` directories only under an age-based
policy that cannot race active drafts. Do not advertise the import as atomic
until file promotion and compensating cleanup are implemented.

## Lyrics

The direct admin endpoint accepts synchronized LRC timestamps in
`[mm:ss]`, `[mm:ss.xx]`, or `[mm:ss.xxx]` form. Each line ends at the next
timestamp; the final line ends at the track duration. Nonblank plain-text lines
are accepted for compatibility and receive synthetic timestamps at five-second
intervals based on source line number. A blank document removes all lyrics.

## Security assumptions

The admin API, including archive import and export, supports an optional shared
secret through `OPENCHORD_ADMIN_API_KEY` and the `X-OpenChord-Admin-Key` request
header. Leaving it unset preserves trusted-local-network behavior. Before
internet exposure, configure the key, place the service behind authenticated
access, and apply request-size limits appropriate for media uploads and library
archives.

Archive import validates ZIP entry paths, declared and extracted sizes,
checksums and manifest references. The application-wide multipart limit
defaults to 20 GB and is configurable through `MAX_UPLOAD_SIZE`; a production
ingress must set a compatible deployment-specific request limit as well.
Streaming export requests use a one-hour async timeout by default, configurable
through `ASYNC_REQUEST_TIMEOUT`, so large libraries are not cut off by the web
framework's short default timeout.

External processes (`ffprobe` and `ffmpeg`) receive argument lists rather than
shell command strings. Staged filenames are generated by the server, and all
catalog media paths are normalized and checked against `MEDIA_ROOT`.

## Configuration and operations

Required production concerns:

- PostgreSQL availability and Flyway migration completion;
- persistent, writable `MEDIA_ROOT` for administration and readable media for
  serving;
- `PUBLIC_BASE_URL` set to the externally reachable origin used in GraphQL
  media URLs;
- FFmpeg and FFprobe installed when album import is enabled;
- authentication at the ingress for `/api/admin/**`;
- monitoring of liveness, readiness, disk capacity, abandoned import data and
  archive staging data;
- backup and reconciliation of PostgreSQL and managed media as one operational
  unit, because filesystem writes cannot join the database transaction.

The container runs unprivileged. Compose mounts `./media` read-write for the API
container because direct uploads and album imports require the process to write
below `MEDIA_ROOT`.
