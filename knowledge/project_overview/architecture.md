# Cola Tracker Architecture Overview

## Project Description
Android application for tracking children's drink consumption (specifically Cola). 
It features a **mock mode** for UI testing and a **real mode** interacting with a FastAPI backend.

## Tech Stack
### Android Client
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Networking**: Ktor Client (CIO engine)
- **Serialization**: Kotlinx Serialization
- **Image Loading**: Coil
- **Navigation**: Jetpack Navigation Compose
- **Build System**: Gradle Kotlin DSL

### Backend
- **Framework**: FastAPI (Python)
- **Server**: Uvicorn
- **Data Storage**: JSON file (`data.json`)
- **Photos**: Local filesystem storage

## Key Architecture Patterns

### Repository Pattern
The app abstracts data sources using the `ColaTrackerRepository` interface.
- `MockRepository`: In-memory monitoring, useful for development without backend.
- `RealRepository`: Implementation connecting to the FastAPI backend via Ktor.

### Configuration
App behavior is toggled via `AppConfig.kt`:
- `USE_MOCK_DATA`: Boolean flag to switch between Mock and Real repositories.

### Navigation
Single Activity architecture (`MainActivity`) hosting a `NavHost`.
- Routes: `children_list`, `child_detail/{childJson}`.
- Arguments are passed as serialized JSON strings.

## Directory Structure
- `data/`: Models, API client, Repositories.
- `domain/`: (Implicit in this simple app, merged with data/viewmodels).
- `ui/`: Compose screens, components, theme.
- `viewmodels/`: State management for screens.
