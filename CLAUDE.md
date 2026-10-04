# CLAUDE.md - Cola Tracker

## Project Overview
Android app for tracking children's cola/drink consumption. Built with Kotlin, Jetpack Compose, and MVVM architecture.

## Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Repository pattern
- **HTTP Client**: Ktor 2.3.7
- **Serialization**: Kotlinx Serialization
- **Navigation**: Jetpack Navigation Compose
- **Image Loading**: Coil 2.5.0
- **Min SDK**: 26 (Android 8.0) | **Target SDK**: 34 (Android 14)

## Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on device/emulator
./gradlew installDebug

# Clean build
./gradlew clean
```

## Project Structure
```
app/src/main/kotlin/com/colatracker/
├── AppConfig.kt              # Configuration (mock/real mode toggle, timeouts)
├── ColaTrackerApp.kt         # Application class (Coil ImageLoader setup)
├── MainActivity.kt           # Entry point + manual navigation
├── data/
│   ├── api/ColaTrackerApi.kt       # Ktor HTTP client + error mapping
│   ├── models/                      # Data models (Child, DrinkHistoryItem, ApiModels)
│   └── repository/                  # Repository (interface, ApiProvider, Mock/Real)
├── ui/
│   ├── screens/              # ChildrenListScreen, ChildDetailScreen, SettingsScreen
│   ├── components/           # Reusable UI (ChildUi, icons, progress bars, skeletons)
│   └── theme/                # Material3 Theme, container palette, Typography
└── viewmodels/               # ChildrenListViewModel, ChildDetailViewModel
```

## Domain Model — READ THIS FIRST

The backend accumulates balance month to month (`check_and_update_limits`):

- `monthly_limit` — **monthly top-up**, not a goal. Added to the balance on the 1st.
- `consumed_this_month` — drunk since the 1st; reset monthly.
- `remaining` — **accumulated balance ("cola meter")**. Never expires, carries over,
  so it can exceed `monthly_limit` and can go negative.

Therefore `remaining != monthly_limit - consumed_this_month`. Month usage and
balance are two independent numbers — never mix them in the UI, and never derive
warning colours from `remaining / monthly_limit`. Derived properties live on
`Child` (`monthUsageRatio`, `monthUsagePercent`, `balanceInMonths`, `isOverdrawn`).

## Key Patterns

### Configuration
- `AppConfig.kt` controls mock vs real API mode via `USE_MOCK_DATA` boolean
- Mock mode uses fake data for UI development without backend
- Real mode connects to FastAPI backend

### Networking
- One `HttpClient` per process via `ApiProvider` — ViewModels must not create their own
- `expectSuccess = true`; `ColaTrackerApi.safeCall` maps HTTP/IO failures to
  user-facing Russian messages in `ApiException`
- `CancellationException` is always rethrown, never converted to an error state

### State Management
- ViewModels use `MutableStateFlow` for UI state
- Sealed classes for type-safe state representation (e.g., `ChildrenUiState`)
- Composables collect state with `collectAsStateWithLifecycle()`
- User-facing messages go through a `message` StateFlow shown in a `Snackbar`

### API Endpoints (via ColaTrackerApi)
- `GET /children` - List all children
- `POST /children/{id}/drink` - Add drink entry
- `GET /children/{id}/history` - Get drink history
- `DELETE /drinks/{id}` - Delete drink entry
- `POST /children/{id}/photo` - Upload photo (multipart)

### Navigation
- No NavHost. `MainActivity.ColaTrackerApp()` switches screens on
  `rememberSaveable` state (selected child **id**, settings flag)
- `BackHandler` handles the system back button; without it back closes the app

### Theming
- Material3 1.1.2 (compose-bom 2024.01.00) has **no** `surfaceContainer*` colour
  roles and **no** `HorizontalDivider`/`OutlinedTextFieldDefaults` — use
  `ColaTheme.containers.*`, `Divider`, and `TextFieldDefaults` instead
- Screens must read colours from `MaterialTheme.colorScheme` / `ColaTheme.containers`,
  never from the raw `Cola*` palette constants, or dark theme breaks

### Images
- Build photo URLs with `childPhotoUrl(photoUrl, version)` — the backend reuses the
  filename `photos/child_{id}.jpg`, so a version query param is required to bust
  Coil's cache after re-upload

## Code Conventions
- Classes: PascalCase
- Functions/variables: camelCase
- Constants in AppConfig: UPPER_SNAKE_CASE
- Comments and documentation: Russian
- Package: `com.colatracker.*`

## Dependencies (Key)
- Compose BOM 2024.01.00
- Ktor Client 2.3.7 (CIO engine)
- Kotlinx Serialization 1.6.2
- Kotlinx Coroutines 1.7.3

## Notes
- No XML layouts - 100% Compose
- ProGuard rules configured for release builds
- Supports light and dark themes
- Cleartext traffic allowed for local API development
