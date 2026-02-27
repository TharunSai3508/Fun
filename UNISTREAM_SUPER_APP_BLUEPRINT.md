# UNISTREAM – Ultimate Media & Content Super Platform

## 1) Product Vision
UNISTREAM combines **three premium experiences in one Android app**:

1. **Gallery (Pinterest-style)**
2. **Streaming (Netflix-style)**
3. **WebNovel Reader (Webnovel-style)**

Each module keeps an independent UI identity while sharing a unified account, security layer, and data engine.

---

## 2) Suggested Tech Stack (Android)

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture + MVVM
- **DI:** Hilt
- **Storage:** Room, DataStore, EncryptedFile, Exo cache
- **Media:** ExoPlayer (Media3), Coil/Glide
- **Security:** BiometricPrompt, Android Keystore, EncryptedSharedPreferences
- **Background jobs:** WorkManager
- **Cloud auth:** Google Sign-In / Credential Manager
- **Networking:** Ktor/Retrofit + OkHttp
- **Parser:** Jsoup (for webnovel extraction)

---

## 3) Global App Foundation

### 3.1 App-Level Security
- App lock gates app launch and selected modules:
  - Fingerprint / face unlock via `BiometricPrompt`
  - PIN fallback
  - Auto-lock timeout
  - Per-module lock (Gallery, Hidden Vault, Streaming, Novel)
- Store lock settings and timeout policy in encrypted preferences.
- Add anti-capture mode for sensitive screens:
  - Use `FLAG_SECURE` for hidden vault and sensitive readers.

### 3.2 Global Navigation
- Root shell with three entry cards/tabs:
  - Gallery
  - Streaming
  - WebNovel
- Each entry routes to an isolated navigation graph.

### 3.3 Profile & Account
- Login with **Google Account** for user profile and sync identity.
- User can connect their own AI provider key/account (e.g., ChatGPT/Gemini) inside Settings.
- Keep AI provider abstraction:
  - `AiProvider.ChatGpt`
  - `AiProvider.Gemini`
  - Future providers

---

## 4) Module 1 — Gallery (Pinterest-style)

### 4.1 UI Identity
- Light/pastel palette
- Masonry staggered grid
- Rounded large thumbnails
- Smooth zoom transitions

### 4.2 Core Media Sections
- All media
- Albums (folder-based)
- Favorites
- Recently Added
- Recently Viewed
- Trash (30-day restore)
- Cloud-backed items
- Hidden folder (secure vault)

### 4.3 Media Features
- Search by filename/date/location/type
- Filters: videos, GIFs, large files, screen recordings
- Large preview detail screen

### 4.4 Image Editor
- Crop, rotate, flip, resize
- Adjustments: brightness, contrast, saturation, warmth, tint, highlights/shadows, sharpness, blur
- Advanced: filters/LUT, text, stickers, draw, mosaic/pixelate

### 4.5 Video Tools
- Trim, crop, mute
- Extract thumbnail
- Convert to GIF
- Speed controls

### 4.6 Hidden Vault (Secure)
- Move selected media to encrypted internal storage
- Remove from MediaStore visibility
- Biometric/PIN-only access
- Optional decoy vault mode
- Screenshot and recording protection inside vault

### 4.7 Wallpaper Engine
- Set image, GIF live wallpaper, video live wallpaper
- Home/lock/both where OEM allows
- Fallback strategy for device limits:
  - If split lock/home media is blocked by OEM, provide best-supported single wallpaper + optional in-app rotating lock overlay guidance
- Parallax effect
- Auto-rotate / slideshow / daily scheduler

---

## 5) Module 2 — Streaming (Netflix-style)

### 5.1 UI Identity
- Dark-first UI
- Hero banners
- Horizontal carousels
- Autoplay previews
- Red accent highlights

### 5.2 Sources
1. Local device media
2. Google Drive public links
3. Direct streaming URLs (MP4, HLS, DASH)

### 5.3 Player Capabilities (Media3)
- Hardware acceleration
- Subtitle support + downloader
- Audio track switching
- Playback speed
- Gesture controls (brightness/volume/seek)
- Picture-in-Picture
- Cast integration
- Background audio mode

### 5.4 Rows/Collections
- Featured
- Trending
- Continue Watching
- Recently Added
- My List
- Downloaded
- History

### 5.5 Parental Controls
- Profile-based restrictions
- Age gate per profile
- Optional lock per profile

---

## 6) Module 3 — WebNovel Platform (Webnovel-style)

### 6.1 UI Identity
- Minimal immersive reader
- Light/dark/sepia themes
- Serif-friendly typography
- Smooth scrolling and optional page mode

### 6.2 Import Pipeline
Input: user-pasted novel URL

Pipeline:
1. Fetch HTML
2. Parse chapter list
3. Extract chapter content
4. Normalize and structure
5. Save in Room DB
6. Download assets for offline reading

### 6.3 Reader Features
- Infinite scroll / page flip
- Font family and size
- Line spacing
- Reading progress auto-save
- Bookmarks, highlights, notes
- Export notes
- Auto-scroll
- Text-to-speech

### 6.4 Auto Chapter Updater
- Periodic checks via WorkManager
- Fetch newly added chapters
- Background download + user notification

---

## 7) Shared Data Model (High Level)

- `UserProfile(id, googleAccountId, displayName, avatarUrl)`
- `SecurityConfig(lockEnabled, timeoutSec, lockedModules, pinHash)`
- `MediaItem(id, uri, type, bucket, tags, isFavorite, isHidden, createdAt)`
- `StreamItem(id, sourceType, url, title, poster, progressMs)`
- `Novel(id, sourceUrl, title, author, cover, lastChapter)`
- `NovelChapter(id, novelId, chapterNumber, title, content, publishedAt)`
- `ReadingState(novelId, chapterId, progress, theme, fontScale)`
- `AiConfig(provider, encryptedApiToken, modelName)`

---

## 8) AI Features (Phased)

### Phase A (Bring-your-own AI account)
- Users attach ChatGPT or Gemini credentials in Settings.
- AI features enabled only after credential validation.

### Phase B (Features)
- AI photo enhancement suggestions
- AI wallpaper generation prompt-to-image (provider-dependent)
- AI novel summarization
- AI subtitle generation

### Safety:
- Explicit user opt-in for remote processing
- Show data-use consent and source content scope
- Allow per-feature disable toggles

---

## 9) Suggested Delivery Roadmap

### MVP (8–12 weeks)
- Module shells + independent themes
- Gallery core browsing + favorites + trash
- Streaming player with local/URL playback
- WebNovel import + reader basics
- Google login
- App lock + biometric/PIN

### V2
- Hidden vault encryption
- Google Drive streaming cache
- Advanced editors
- Parental controls
- Chapter auto-updates

### V3
- AI provider integration (BYO keys)
- AI enhancement features
- Cloud sync and web dashboard

---

## 10) Engineering Notes
- Keep each module in separate packages and nav graph for long-term maintainability.
- Define common design tokens but allow per-module theme overrides.
- Treat vault and lock screens as hardened surfaces (secure window flags, no previews in recents).
- For OEM wallpaper limitations, provide capability detection + fallback UX instead of forcing unsupported behavior.

