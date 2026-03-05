# UNISTREAM – Ultimate Media & Content Super Platform

UNISTREAM is a Kotlin + Jetpack Compose Android Super App combining:

- 📸 **Gallery** (Pinterest-inspired media experience)
- 🎬 **Streaming** (Netflix-inspired playback experience)
- 📖 **WebNovel** (Webnovel-inspired reading experience)

## Current Focus
This repository is moving from prototype to stable MVP with emphasis on:

1. Security-first access (app lock + vault lock)
2. Reliable local media + URL ingestion flows
3. Clear modular UI identity for each module
4. Room-backed offline-first data for streaming and novels

## Implemented Highlights

### Global
- Jetpack Compose architecture with module-based navigation
- App lock flow with biometric + PIN fallback
- Runtime media permission request on first launch
- Settings/Profile screen with Google sign-in entrypoint

### Gallery
- MediaStore browsing (images/videos/GIF)
- Filtering, search, albums, media detail
- Hidden vault with first-time PIN setup and biometric unlock support
- Wallpaper apply:
  - Images -> system/lock/both
  - Video/GIF -> live wallpaper service flow

### Streaming
- Device video ingestion
- URL entry for direct streams + basic web-page source extraction
- ExoPlayer playback screen with custom controls
- Continue watching + playlist/watch history storage
- Route encoding fix for URLs (prevents navigation/player crashes)

### WebNovel
- URL import to parser pipeline
- Chapter list + chapter content extraction
- Room persistence for novels, chapters, bookmarks, progress
- Reader viewmodel state with history/bookmarks/progress logic

## Architecture

- `core/` — app theme, security, DI, navigation, database
- `gallery/` — media browsing, hidden vault, wallpaper flows
- `streaming/` — player, playlists, watch history, URL sources
- `webnovel/` — parser, repository, library/import/reader
- `settings/` — profile and app-level settings surface

## Build

> Note: this repository currently relies on system `gradle` in this environment.

```bash
gradle :app:assembleDebug
```

If you are setting up locally, ensure:
- Android SDK is installed
- Compatible JDK for Android Gradle Plugin is used
- Add/restore Gradle Wrapper (`gradlew`) for reproducible builds

## Product Goals Roadmap

### Phase 1 (Stability)
- Harden lock lifecycle, permission handling, and playback error states
- Improve stream/novel URL extraction reliability
- Add test coverage for parser/repository/viewmodel flows

### Phase 2 (Core Feature Completion)
- Complete editor and video tools in Gallery
- Improve streaming download/caching and subtitle pipelines
- Improve novel auto-update worker + notifications

### Phase 3 (Premium UX)
- Stronger module-specific design systems and motion
- Accessibility and performance tuning

### Phase 4 (AI + Cloud)
- AI provider integration via user-provided keys (OpenAI/Gemini)
- Cloud sync and social/web dashboard expansion

## Security Notes
- Vault and app lock use encrypted preference storage.
- Biometric availability depends on enrolled device credentials.
- Users should set vault PIN on first hidden-vault access.

## Known Limitations
- Some web providers (e.g., YouTube/Instagram protected pages) may not expose directly playable URLs without provider-specific extraction APIs.
- Live wallpaper behavior can differ by OEM/device restrictions.
