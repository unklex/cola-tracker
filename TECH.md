# Cola Tracker — Technical Documentation

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 1.9.22 |
| UI Framework | Jetpack Compose | BOM 2024.01.00 |
| Design System | Material Design 3 | via Compose BOM |
| Architecture | MVVM + Repository | — |
| HTTP Client | Ktor (CIO engine) | 2.3.7 |
| Serialization | Kotlinx Serialization | 1.6.2 |
| Coroutines | Kotlinx Coroutines | 1.7.3 |
| Image Loading | Coil | 2.5.0 |
| Navigation | Jetpack Navigation Compose | 2.7.6 |
| Build Tool | Gradle | 8.13 |
| AGP | Android Gradle Plugin | 8.13.2 |
| Min SDK | 26 (Android 8.0) | — |
| Target SDK | 34 (Android 14) | — |

## Architecture

```
┌─────────────────────────────────────┐
│            UI Layer                 │
│  Screens → Components → Theme      │
│  (Jetpack Compose, Material 3)      │
├─────────────────────────────────────┤
│         ViewModel Layer             │
│  StateFlow → Sealed UI States       │
│  (ChildrenListVM, ChildDetailVM)    │
├─────────────────────────────────────┤
│        Repository Layer             │
│  ColaTrackerRepository (interface)  │
│  ├── RealRepository (API)           │
│  └── MockRepository (fake data)     │
├─────────────────────────────────────┤
│          Data Layer                 │
│  ColaTrackerApi (Ktor HttpClient)   │
│  Models: Child, DrinkHistoryItem    │
└─────────────────────────────────────┘
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/children` | Список всех детей с данными потребления |
| POST | `/children/{id}/drink` | Добавить запись напитка |
| GET | `/children/{id}/history` | История напитков ребёнка |
| DELETE | `/drinks/{id}` | Удалить запись напитка |
| POST | `/children/{id}/photo` | Загрузить фото (multipart) |
| GET | `/` | Health check |

**Backend**: FastAPI (Python), IP: `<SERVER_IP>:8000`, HTTP (cleartext)

## Project Structure

```
app/src/main/kotlin/com/colatracker/
├── AppConfig.kt                    # USE_MOCK_DATA, BASE_URL, AUTH_TOKEN
├── MainActivity.kt                 # Entry point, Coil setup, navigation
├── data/
│   ├── api/ColaTrackerApi.kt       # Ktor client, all endpoints
│   ├── models/
│   │   ├── Child.kt                # @Serializable, consumptionProgress
│   │   ├── DrinkHistoryItem.kt     # @Serializable, date formatting
│   │   └── ApiModels.kt            # Request/Response DTOs
│   └── repository/
│       ├── ColaTrackerRepository.kt # Interface
│       ├── RealRepository.kt        # API-backed
│       └── MockRepository.kt        # Fake data for dev
├── ui/
│   ├── screens/
│   │   ├── ChildrenListScreen.kt   # Home: Effervescent Archive design
│   │   └── ChildDetailScreen.kt    # Detail: progress, history, add drink
│   ├── components/
│   │   ├── ChildCard.kt            # Card for child (used in detail)
│   │   ├── CircularProgressAvatar.kt # Animated circular progress + avatar
│   │   ├── ColaIcons.kt            # Canvas-drawn icons (glass, bottle, can)
│   │   ├── ColaTopAppBar.kt        # Gradient top bar (detail screen)
│   │   ├── GradientProgressBar.kt  # Animated progress bars
│   │   ├── ShimmerEffect.kt        # Loading skeleton animations
│   │   └── StatisticsCard.kt       # Weekly chart, stats row
│   └── theme/
│       ├── Theme.kt                # Effervescent Archive palette, gradients
│       └── Type.kt                 # Typography
└── viewmodels/
    ├── ChildrenListViewModel.kt    # Children list state
    └── ChildDetailViewModel.kt     # Detail state, drinks CRUD, photo upload
```

## Permissions

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | API requests |
| `ACCESS_NETWORK_STATE` | Network availability check |
| `CAMERA` | Photo capture for avatars |
| `READ_MEDIA_IMAGES` | Gallery photo selection |

## Build Commands

```bash
# Сборка debug APK
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" \
  "C:/Program Files/Android/Android Studio/jbr/bin/java.exe" \
  -cp gradle/wrapper/gradle-wrapper.jar \
  org.gradle.wrapper.GradleWrapperMain assembleDebug

# Или через Android Studio: Build → Build APK(s)
```

**Output**: `app/build/outputs/apk/debug/app-debug.apk` (~11 MB)

## Configuration

`AppConfig.kt` flags:
- `USE_MOCK_DATA: Boolean` — переключение между mock и real API
- `BASE_URL: String` — адрес бэкенда
- `AUTH_TOKEN: String` — Bearer-токен авторизации
- `REQUEST_TIMEOUT_MS: Long` — таймаут запросов (30 сек)

## State Management

- ViewModels используют `MutableStateFlow` для UI-состояния
- Sealed class `ChildrenUiState`: Loading, Success(children), Error(message)
- Sealed class `ChildDetailUiState`: Success(child, history), Error(message)
- Composables собирают стейт через `collectAsStateWithLifecycle()`

## Navigation

Ручная навигация через sealed class `Screen` в `MainActivity`:
- `Screen.ChildrenList` → `Screen.ChildDetail(child)`
- Без NavHost (во избежание layout crash при быстрой навигации)

## Design System: Effervescent Archive

Палитра основана на "editorial magazine" стиле:
- **Primary**: #BA0012 (Cola Red) → #A4000F (dim)
- **Tertiary**: #7842A5 (purple accent) → #D199FF (container)
- **Surface**: #FAF5F5 → #ECE7E7 → #E6E1E1 → #E0DCDC (layered)
- **No-Line Rule**: границы через цвет фона, не borders
- **Rounded corners**: minimum 0.5rem, preferred 1.5rem
- **Shadows**: ultra-diffused, rgba(48,46,47,0.06)
