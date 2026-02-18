# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RC Download is a dual-platform YouTube downloader: an Android app (Kotlin/MVVM) that talks to a local Python Flask + yt-dlp backend service. Documentation in the README is in Portuguese.

## Commands

### Android App (`android-app/`)

```bash
# Build debug APK
./gradlew build

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Format code
./gradlew spotlessApply
```

### Python Service (`local-service/`)

```bash
# First-time setup (creates venv, installs deps, validates ffmpeg)
chmod +x start.sh && ./start.sh

# Run tests with coverage
source .venv/bin/activate
pytest tests/ -v --cov=app

# Run a single test
pytest tests/test_app.py::TestClassName::test_method_name -v

# Start the service manually
python app.py  # listens on 0.0.0.0:8080
```

**System requirement**: `ffmpeg` must be installed for MP3 audio extraction.

## Architecture

### Android: MVVM + Repository Pattern

```
MainActivity / HistoryActivity  (View Binding, observe uiState Flow)
        ↓
MainViewModel  (UiState sealed class, viewModelScope coroutines)
        ↓
VideoRepository  (single access point, returns Result<T>)
        ↓
├── YouTubeApiService (Retrofit → YouTube Data API v3)
├── LocalDownloadService (Retrofit → local Flask at 10.0.2.2:8080)
└── DownloadHistoryDao (Room database)
```

**UiState states**: `Idle → Loading → MetadataLoaded → Downloading → Progress → Complete / Error`

**Dependency injection** is manual via `MainViewModelFactory` (no Hilt). It constructs OkHttpClient, both Retrofit instances, and the repository.

**URL parsing**: `YouTubeUrlValidator` extracts the 11-character video ID via regex, used before any API call.

### Python Backend (`local-service/app.py`)

Three endpoints:
- `POST /download` — validates request, spawns a worker thread, returns `job_id`
- `GET /status/<job_id>` — returns progress 0–100 and status from in-memory `jobs` dict
- `GET /health` — health check

Downloads are non-blocking (threading). yt-dlp progress hooks update the in-memory job state. Output goes to `~/Downloads/rc-download/`.

### Configuration / Secrets

- Android secrets (`YOUTUBE_API_KEY`, `LOCAL_SERVICE_URL`) are stored in `android-app/local.properties` (git-ignored) and injected into `BuildConfig` via `app/build.gradle.kts`. Copy `local.properties.example` to get started.
- The emulator reaches the host machine at `10.0.2.2:8080`. Cleartext HTTP is allowed only for that host in `AndroidManifest.xml`.
- All other network traffic (YouTube API) uses HTTPS.

### Key Files

| File | Purpose |
|------|---------|
| `android-app/app/src/main/java/com/rcdownload/MainViewModel.kt` | State machine and coroutine logic |
| `android-app/app/src/main/java/com/rcdownload/data/VideoRepository.kt` | Orchestrates all data sources |
| `android-app/app/src/main/java/com/rcdownload/MainViewModelFactory.kt` | Manual DI wiring |
| `android-app/gradle/libs.versions.toml` | Centralized dependency versions |
| `local-service/app.py` | Entire Flask service (152 lines) |
| `local-service/tests/test_app.py` | Python test suite (281 lines) |
