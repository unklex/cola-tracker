---
description: Build, Test, and Run the Cola Tracker application
---

# Build and Run Workflow

## Android Application

### 1. Build Debug APK
```bash
./gradlew assembleDebug
```

### 2. Run Unit Tests
```bash
./gradlew test
```

### 3. Clean Build
```bash
./gradlew clean
```

## Backend Server

### 1. Start Server
Run this in a separate terminal:
```bash
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
*Note: Ensure `uvicorn` and `fastapi` are installed (`pip install fastapi uvicorn`).*

## Troubleshooting
- **Gradle Sync Failed**: Run `File -> Invalidate Caches` in Android Studio.
- **Connection Refused**: Check if your phone/emulator is on the same network as the PC running the backend, and that `AppConfig.kt` has the correct IP.
