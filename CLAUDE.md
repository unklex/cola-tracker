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
├── AppConfig.kt              # Configuration (mock/real mode toggle)
├── MainActivity.kt           # Entry point with Navigation
├── data/
│   ├── api/ColaTrackerApi.kt       # Ktor HTTP client
│   ├── models/                      # Data models (Child, DrinkHistoryItem, ApiModels)
│   └── repository/                  # Repository pattern (interface + Mock/Real implementations)
├── ui/
│   ├── screens/              # ChildrenListScreen, ChildDetailScreen
│   ├── components/           # Reusable UI (ChildCard)
│   └── theme/                # Material3 Theme, Typography
└── viewmodels/               # ChildrenListViewModel, ChildDetailViewModel
```

## Key Patterns

### Configuration
- `AppConfig.kt` controls mock vs real API mode via `USE_MOCK_DATA` boolean
- Mock mode uses fake data for UI development without backend
- Real mode connects to FastAPI backend

### State Management
- ViewModels use `MutableStateFlow` for UI state
- Sealed classes for type-safe state representation (e.g., `ChildrenUiState`)
- Composables collect state with `collectAsStateWithLifecycle()`

### API Endpoints (via ColaTrackerApi)
- `GET /children` - List all children
- `POST /children/{id}/drink` - Add drink entry
- `GET /children/{id}/history` - Get drink history
- `DELETE /drink/{id}` - Delete drink entry

### Navigation
- NavHost with routes: `"children_list"` -> `"child_detail/{childJson}"`
- Child objects serialized to JSON for navigation arguments

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
