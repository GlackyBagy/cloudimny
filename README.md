<h1>Cloudimny</h1>
<h4>A self-hosted personal music service: your own server, your own library, streamed to your own Android app.</h4>

## Features

- **One-tap server setup.** Point the Android app at a fresh Linux VPS over SSH and it installs Docker, generates a TLS certificate, and brings up the whole stack — no manual server work.
- **Library management.** Upload tracks, edit title/artist metadata, delete tracks, build playlists.
- **Covers are automatically resolved** via [MusicBrainz](https://musicbrainz.org) and [CoverArtArchive](https://coverartarchive.org).
- **Mirror playback.** Point the app at another music app's now-playing notification and it matches the track against your library and mirrors playback — use for whatever you want.
- **Server sharing.** Export configuration file from settings and use it on another device of yours.
- **IPv6-preview available.** Server addresses accept both IPv4 and bracketed IPv6 literals throughout setup and settings. *But the feature is unstable yet.*

## Getting started
1. Install the Android app (APK from [Releases](../../releases))
2. On first launch, enter the SSH credentials for a fresh Linux (Debian/Ubuntu preferred) server (root or sudo access
   required). None of credentials but address will be saved. The app runs [`setup-script.bash`](setup-script.bash) over that connection, which:
   - installs Docker,
   - generates a self-signed TLS certificate for the address you gave it,
   - deploys the Compose stack,
   - hands back the certificate fingerprint and an access token, both stored on-device.
3. Done — the app is now talking to your server.

## Notes

**0. App says it cannot connect to the server right after setup, why?**

The API container may briefly crash-restart while Postgres/MinIO are still starting up — that's normal,
it recovers on its own in under a couple of minutes. The app doesn't retry automatically though: pull to refresh (or reopen the screen) once it's had time to settle.

**1. Can I export my music collection?**

The feature is not available yet. But you can download docker volumes from the server.

Connect to the server via SSH, stop the stack for a consistent snapshot, then archive the two volumes that
actually hold your data — `minio_data` (audio files) and `sql_data` (metadata):
```bash
cd /opt/cloudimny && docker compose stop
docker volume ls | grep -E 'minio_data|sql_data'   # confirm the exact (project-prefixed) names
docker run --rm -v <volume_name>:/data:ro -v "$PWD/backup":/backup alpine tar czf /backup/<volume_name>.tar.gz -C /data .
docker compose start
```
Repeat the `docker run` line for both volumes.

Both archives end up in `/opt/cloudimny/backup`, named `minio_data.tar.gz` and `sql_data.tar.gz`.

**2. Can I use multiple servers?**

No.

**3. Can I revoke access to the server from some devices?**

Yes. Just re-setup the server (clear the app data — not only cache — in the Android settings to see the
setup page). This action will revoke access from **all** other devices. Your collection won't be affected.

## Architecture

```
Android app  →  Caddy (TLS, :443)  →  Spring Boot API  →  PostgreSQL (metadata)
                                                       →  MinIO (media storage, S3-compatible)
```

Backend app runs in Docker Compose on a single server ([compose.yaml](compose.yaml)):
Caddy terminates TLS with the self-signed certificate `setup-script.bash` generates, and reverse-proxies
to the API container. The API stores track/playlist metadata in Postgres and the audio files
themselves in MinIO. Services are inaccessible for unauthorized users.

## Tech stack

- **Android app** — Kotlin, media3/ExoPlayer, Retrofit, SSHJ
- **API** — Spring Boot (Java), PostgreSQL, MinIO
- **Infrastructure** — Docker Compose, Caddy

### Why WebFlux?
I've used this project for educational purposes, so the stack leans toward what teaches something new
rather than what a single-user self-hosted service strictly needs.

## License

[MIT](LICENSE)
