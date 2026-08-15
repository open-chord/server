# OpenChord server

Java 21 / Spring Boot backend for the self-hosted OpenChord music client. It
owns accounts, the music catalog, playlists, synchronized lyrics, playback
history, portable archives, and AVPlayer-compatible media delivery.

## Quick start

The default Compose stack starts PostgreSQL, the API, and the separate React
administration client:

```sh
docker compose up --build
```

Open `http://localhost:8080/admin/` and create the initial owner account. The
first setup request also selects the server mode:

- `PERSONAL` keeps public registration closed;
- `FAMILY` allows member registration while reserving administration routes
  for the owner.

For an iPhone on the same network, the helper detects the host LAN address,
sets the public media URL, and prints the address to enter in the app:

```sh
./scripts/lan-up.sh
```

If automatic detection is unavailable, provide the address explicitly:

```sh
OPENCHORD_LAN_IP=192.168.1.20 ./scripts/lan-up.sh
```

Compose persists PostgreSQL in a named volume and mounts `./media` for catalog
uploads. The database is exposed on `${POSTGRES_PORT:-5432}` for local tooling.
Flyway installs the schema and a small demo catalog whose audio lives under
`media/demo`.

For development with the application running directly on the host:

```sh
docker compose up -d database
./mvnw spring-boot:run
```

## Authentication and API

The server uses opaque bearer sessions. Access tokens expire after 20 minutes;
refresh tokens expire after 30 days and rotate on refresh. Send protected API
requests with `Authorization: Bearer <access-token>`. Media URLs may use the
same token as the `access_token` query parameter because AVPlayer cannot attach
arbitrary request headers.

Public bootstrap endpoints are under `/api/auth`: server discovery, initial
setup, registration, login, and refresh. All other endpoints require a valid
session; `/api/admin/**` additionally requires the `OWNER` role.

Primary contracts:

- `POST /graphql` — catalog queries, playlists, and playback mutations;
- `GET /media/tracks/{id}` — audio with single-range HTTP support;
- `GET /media/artwork/{id}` — album or playlist artwork;
- `GET /api/admin/catalog` — administration catalog projection;
- `POST /api/admin/imports/analyze` — stage an album folder and inspect tags;
- `POST /api/admin/imports/{id}/commit` — commit reviewed metadata and media;
- `GET|PUT /api/admin/tracks/{id}/lyrics` — inspect or publish synchronized lyrics;
- `POST /api/admin/tracks/{id}/lyrics/alignment` — start optional automatic alignment;
- `GET /api/admin/openchord/export` — stream a portable library or playlist archive;
- `POST /api/admin/openchord/import` — validate and import an archive;
- `GET /actuator/health/liveness` and `/readiness` — operational probes.

The authoritative GraphQL contract is
[`src/main/resources/graphql/schema.graphqls`](src/main/resources/graphql/schema.graphqls).
Durations and lyric timestamps are integer milliseconds.

## Smart album import

The administration client accepts a folder containing audio and optional cover
artwork. FFprobe reads embedded artist, album, year, disc, track, title, and
duration metadata before catalog mutation. The review screen exposes detected
inconsistencies. On commit, FLAC/WAV/AIFF sources become ALAC in an M4A
container; compatible compressed sources remain unchanged.

The database commit is transactional, but file copies and FFmpeg output are
not. Failed imports can leave unreferenced files under `MEDIA_ROOT`. See
[`docs/architecture.md`](docs/architecture.md#album-import-lifecycle) for the
complete consistency boundary.

## Automatic lyrics alignment

Set `LYRICS_ALIGNER_URL` to a local `whisper.cpp` HTTP server. OpenChord uploads
managed audio to `/inference`, aligns timestamped recognized words against the
saved source text, and stores the result for human review. Without this setting,
manual source and LRC editing remain available.

For the bundled CPU-oriented setup, download the multilingual `small` model and
start the lyrics overlay:

```sh
./scripts/download-whisper-model.sh small
docker compose -f compose.yaml -f compose.lyrics.yaml up --build
```

Model files remain in `models/whisper` on the host and are excluded from the
application image. The overlay defaults to the amd64 image because some macOS
Docker virtual machines raise `SIGILL` with the current arm64 server. Native
Linux arm64 deployments can set
`WHISPER_IMAGE=ghcr.io/ggml-org/whisper.cpp:main-arm64` and
`WHISPER_PLATFORM=linux/arm64`.

## Portable `.openchord` archives

The administration API implements draft format `0.1`. Whole-library exports
include albums, tracks, playlists, ordering, artwork, and managed media.
Playlist exports include the selected playlist and its dependency closure.

Imports treat ZIPs as untrusted input: normalized paths, entry and archive
limits, declared and extracted byte lengths, SHA-256 digests, and manifest
references are validated before catalog entities are committed. The current
catalog persists one album artist rather than arbitrary credit roles; richer
archive credits are reduced to the album artist or primary credit on import.

## Configuration

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/openchord` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `openchord` | Database credentials |
| `MEDIA_ROOT` | `./media` | Managed audio, artwork, and staging root |
| `PUBLIC_BASE_URL` | `http://localhost:8080` | Origin used in client media URLs |
| `MAX_UPLOAD_SIZE` | `20GB` | Multipart file and request limit |
| `ASYNC_REQUEST_TIMEOUT` | `1h` | Streaming archive response timeout |
| `LYRICS_ALIGNER_URL` | empty | Optional local whisper.cpp endpoint |

Stored media paths are relative to `MEDIA_ROOT`; resolution outside that root
is rejected. Production deployments need a persistent shared volume or a
future object-storage adapter, plus ingress request limits compatible with the
application limit.

## Source layout

The code is organized by product boundary rather than technical layer:

- `auth` — bootstrap, users, opaque sessions, and roles;
- `catalog` — artist, album, track, and lyric persistence model;
- `playlist`, `playback` — user-owned collections and listening history;
- `graphql`, `media` — client-facing query and binary delivery transports;
- `admin` — direct owner-only catalog operations;
- `admin.importing`, `admin.archive`, `admin.lyrics` — independent administration workflows;
- `config` — typed runtime settings and framework wiring.

Deeper transaction, filesystem, and security decisions are documented in
[`docs/architecture.md`](docs/architecture.md).

## Quality gates

```sh
./mvnw spotless:check
./mvnw -DskipTests compile
./mvnw test
docker build -t openchord-back:local .
```

Tests use Testcontainers with PostgreSQL 17 rather than an in-memory database.
GitHub Actions reports formatting, static analysis, tests, and container build
as separate checks. The runtime image is unprivileged; Compose grants the API
write access to `./media` for administration workflows.

## Further documentation

- [`docs/architecture.md`](docs/architecture.md) — ownership, invariants,
  request flows, transaction boundaries, and operational caveats;
- [`src/main/resources/db/migration`](src/main/resources/db/migration) —
  authoritative database schema and seed history;
- [`src/main/resources/graphql/schema.graphqls`](src/main/resources/graphql/schema.graphqls)
  — authoritative public GraphQL contract.
