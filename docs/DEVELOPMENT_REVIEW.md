# UNISTREAM Development Review (Current State)

## Scope reviewed
- Android app structure, dependencies, and feature implementation under `app/src/main`.
- Current implementation compared against the UNISTREAM product goal:
  - Gallery (Pinterest-style)
  - Streaming (Netflix-style)
  - WebNovel (Webnovel-style)
  - Global security/premium app behavior

## Executive summary
UNISTREAM already has a strong **modular foundation** with separate packages for gallery, streaming, webnovel, and core services. Navigation routes, Compose screens, Room entities/DAOs, and Hilt setup are in place.

However, the app is currently at an **early-to-mid prototype stage**:
- UI shells and partial interaction flows exist.
- Several core data and playback pathways are implemented.
- Many premium/advanced requirements are not yet wired end-to-end.
- Some platform/security details still require hardening.

---

## What is already implemented

### 1) Platform foundation (good progress)
- Kotlin + Jetpack Compose app setup with Material3 and animation libs.
- Hilt DI configured for repositories, DB, and networking.
- Room database includes entities for hidden media, playlists/history, novels/chapters/bookmarks/progress.
- Navigation graph includes app lock, home, and dedicated routes per module.

### 2) Global shell + module separation (good progress)
- Home screen provides entry points for Gallery, Streaming, and Novels.
- Distinct module screen packages and route definitions are present.
- Module-level visual differentiation is started (e.g., colorful Gallery cards, red-accent streaming styling, reader-focused novel UI structure).

### 3) Security baseline (partial)
- EncryptedSharedPreferences is used for PIN/security settings.
- Biometric authentication manager + app lock ViewModel are integrated.
- Support for app PIN, fake vault PIN, per-module lock preferences, and auto-lock timeout values is present in preferences layer.

### 4) Gallery module (partial)
- MediaStore querying for images/videos/GIFs and album-like grouping logic exists.
- Hidden vault entity + DAO + repository methods are present.
- Screens exist for gallery, albums, detail, editor, hidden vault, and wallpaper.

### 5) Streaming module (partial)
- Media3/ExoPlayer integration is present.
- Player screen includes custom controls (seek/playback/speed, overlay controls).
- Playlist + watch history entities/DAO/repository exist.
- Streaming home and playlist screens are present.

### 6) WebNovel module (partial to strong prototype)
- URL import flow exists via parser + repository.
- Novel/chapter persistence model is in place.
- Reader state includes bookmarks, progress tracking, and configurable reader settings.
- Library/import/reader screens and ViewModel orchestration are present.

---

## Gap analysis vs product goal

## A. Global App Features

### A1. App-level lock/security
**Status: Partial**
- Biometric + PIN pathways exist.
- Fake vault mechanics are represented in preference logic.
- App lock can be shown at startup.

**Gaps / risks**
- Auto-lock lifecycle enforcement is not fully wired (activity pause has TODO comment).
- Face unlock is indirectly possible via BiometricPrompt class support, but UX copy/settings are not explicitly exposed.
- Section lock policies exist in storage but are not fully enforced at every navigation boundary.

### A2. Premium UI system
**Status: Partial**
- Distinct visual identities are started, but still need deeper fidelity (motion system, typography system, design tokens and screen-level consistency).

---

## B. Gallery (Pinterest-style)

### Implemented baseline
- Device media browsing and filters (images/videos/GIF).
- Album and media detail pathways.
- Hidden media model + secure settings groundwork.

### Missing / incomplete for target
- Truly polished masonry behavior and transition choreography need production refinement.
- Full editor suite (crop/rotate/resize/adjustments/filters/LUT/text/stickers/mosaic) appears not fully implemented end-to-end.
- Video editing suite (trim/crop/mute/extract thumb/gif/speed) needs completion.
- Hidden vault encrypted file pipeline + MediaStore invisibility guarantees need verification/hardening.
- Screenshot/screen-record protection in vault requires explicit secure-window handling.
- Wallpaper engine advanced features (GIF/video live wallpaper, scheduler, slideshow, parallax, lock/home split compatibility strategies) remain mostly roadmap work.

---

## C. Streaming (Netflix-style)

### Implemented baseline
- Home + player architecture present.
- ExoPlayer supports core playback and speed/seek controls.
- Data structures for history/playlist and progress updates exist.

### Missing / incomplete for target
- Rich source ingestion is incomplete:
  - robust Google Drive public streaming pipeline,
  - direct URL format detection with resilient fallback,
  - offline cache/download manager.
- Full MX-like feature parity pending:
  - subtitle search/download workflow,
  - audio track selection UI,
  - advanced gestures (volume/brightness swipes),
  - cast support UX integration,
  - PiP/background audio lifecycle hardening.
- Netflix-grade rows (Featured/Trending/Continue/My List/etc.) need backend/data model completion and recommendation logic.
- Parental controls/profile locking are not complete yet.

---

## D. WebNovel (Webnovel-style)

### Implemented baseline
- URL import + chapter list parsing + chapter download-on-demand are in place.
- Reader progress + bookmarks + favorites + history data are reasonably scaffolded.

### Missing / incomplete for target
- Parsing reliability across multiple novel sites requires pluggable extractor architecture and stronger sanitization/error handling.
- Reader feature depth pending:
  - robust page-flip mode,
  - TTS integration,
  - high-quality notes/highlights export.
- Auto chapter updater with background periodic work + notifications needs full implementation.
- Theme/typography presets need refinement for premium reading comfort.

---

## Technical concerns to address early

1. **Build reproducibility**
   - Repository currently lacks a `gradlew` script. Developer onboarding/CI reliability is reduced.

2. **Runtime/service configuration hardening**
   - Validate service permissions/exports and media session service declarations for least privilege and platform correctness.

3. **Security hardening**
   - Enforce lock checks at navigation boundaries (not only startup).
   - Implement timeout-based relock from foreground/background events.
   - Add `FLAG_SECURE` only for vault-sensitive surfaces.

4. **Quality gates**
   - Add unit tests for parser/repository logic.
   - Add instrumentation/UI tests for lock flows, media queries, and reader progress restore.

---

## Suggested execution roadmap

### Phase 1 — Stabilization (must-have)
- Add Gradle wrapper and CI build/test baseline.
- Finish lock lifecycle behavior + per-module lock interception.
- Complete core playback reliability (errors, buffering, resume).
- Add basic telemetry/logging and crash observability hooks.

### Phase 2 — Feature completeness (core promise)
- Gallery: complete basic editor + hidden vault file pipeline.
- Streaming: complete source ingestion (Drive/URL/HLS/DASH) and continue-watching correctness.
- Novel: improve import robustness, reading modes, and updater worker.

### Phase 3 — Premium polish
- Distinct design systems per module with motion/spacing/typography tokens.
- Performance optimization (paging, image decoding, prefetch/caching strategies).
- Accessibility pass (TalkBack labels, contrast, font scaling, focus order).

### Phase 4 — Advanced + AI extensions
- AI features behind provider abstraction (user-supplied OpenAI/Gemini API keys).
- Google account profile sync and optional cloud backup.
- Social/web dashboard integration.

---

## Recommendation
UNISTREAM should be treated as a **well-structured v0 foundation** rather than a near-production build. The architecture decisions are promising, and all three modules are represented, but the next milestone should focus on **stability + security + end-to-end completion of core user journeys** before adding broad AI or social surfaces.
