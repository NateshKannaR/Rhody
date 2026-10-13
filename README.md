# RoadPulse - Native Android Driving & Riding GPS Tracker

RoadPulse is a high-performance, privacy-first native Android GPS tracker engineered specifically for motorcycles, cars, and performance driving. Inspired by Enroad, RoadPulse provides real-time GPS telemetry, an interactive speedometer, persistent background recording via an Android 14-compliant location foreground service, offline Room database storage, interactive vector map route tracking via MapLibre Native, high-resolution speed-versus-distance analysis charts, and GPX/CSV route export.

---

## 🚀 Key Features

- **Real-Time Speedometer (km/h)**: High-contrast circular analog/digital speedometer gauge with dynamic color shifting (Cyan → Amber → Racing Red), acceleration arcs, and session peak speed indicators.
- **Accurate Telemetry Engine**:
  - Live speed in km/h (`metersPerSecond * 3.6`).
  - Total distance in kilometers calculated via geodesic Haversine formulas.
  - Maximum credible speed detection.
  - Moving average speed versus overall average speed distinction.
  - Moving duration, elapsed duration, and paused duration metrics.
- **GpsQualityFilter**:
  - Rejects inaccurate GPS fixes (> 25m horizontal accuracy threshold).
  - Filters impossible teleportation jumps (> 250 km/h or impossible delta displacement).
  - Eliminates stationary GPS drift at traffic lights or when parked (< 1.5m at < 0.5 m/s).
  - Rejects duplicate timestamps and out-of-order time-travel fixes.
  - Rejects invalid coordinates (|lat| > 90°, |lon| > 180°, or NaN).
  - Derives speed from geodesic distance and time delta if device fix omits speed.
- **Android 14+ Compliant Location Foreground Service**:
  - Declared with `android:foregroundServiceType="location"`.
  - Promoted immediately to foreground in `onStartCommand` to prevent Android ANR/service launch exemptions.
  - Acquires `PowerManager.PARTIAL_WAKE_LOCK` for continuous, uninterrupted tracking during screen-off and pocket lock.
  - Rich notification with live speed, distance, timer, and interactive `Pause` / `Resume` / `Finish` action buttons.
  - Recovers active or paused trips automatically on app reopen or system restart.
- **Offline Local Storage (Room & SQLite)**:
  - `trips` table with comprehensive aggregate statistics and foreign-key cascaded `track_points` table.
  - Incrementally flushes accepted location fixes without blocking the main UI thread.
  - 100% on-device storage with zero tracking SDKs, no cloud dependency, and no user login.
- **Interactive Vector Maps (MapLibre Native)**:
  - MapLibre Native SDK integration with smooth route polyline rendering.
  - Pre-configured with OpenFreeMap Liberty (free vector tiles, zero API keys required).
  - Camera auto-follow and manual recenter controls.
  - Decoupled from GPS recording engine: if network is unavailable or tiles fail to render, GPS tracking continues completely uninterrupted.
- **Interactive Speed-versus-Distance Graph**:
  - Compose Canvas interactive chart plotting speed (km/h) against cumulative distance (km).
  - Touch scrubbing / inspection: drag along the chart to view precise speed, distance, and timestamps at any recorded point.
  - Peak speed indicator dot and callout badge.
  - Smooth gradient area fill with gridlines and axes labels.
  - Robust against empty, single-point, or short datasets.
- **Export & Sharing**:
  - **GPX 1.1 Export**: Generates compliant GPX XML with `<trk>`, `<trkseg>`, `<trkpt>`, elevation `<ele>`, ISO 8601 UTC timestamp `<time>`, and extension attributes (`<speed>`, `<accuracy>`, `<cumulativeDistance>`).
  - **CSV Export**: Clean CSV with metadata header and columnar track point rows.
  - Shared directly via Android FileProvider and system share sheet.

---

## 🏗️ Architecture & Technology Stack

```
RoadPulse
├── data
│   ├── database       # Room Entities (TripEntity, TrackPointEntity), DAOs, Database
│   └── repository     # TripRepository, Coroutine Dispatchers, local transactions
├── domain
│   ├── calculator     # TripCalculator (Haversine geodesic math, speed, time conversions)
│   ├── filter         # GpsQualityFilter & FilterConfig (jump & jitter filtering)
│   └── model          # TripStatus, GpsStatus, GpsLocationFix, TripMetrics
├── service            # TripRecordingService (Foreground Service, WakeLock, FusedLocationProviderClient)
│                      # TripRecordingController (StateFlow event bus, singleton coordinator)
├── export             # GpxExporter, CsvExporter, ExportManager (FileProvider)
└── ui
    ├── components     # SpeedometerGauge, StatCard, GpsStatusBadge, InteractiveSpeedChart, RouteMapView
    ├── map            # MapConfig (OpenFreeMap style configuration)
    ├── navigation     # Screen definitions, BottomBar
    ├── screens        # Dashboard, LiveTrip, Analytics, History, TripDetail
    └── theme          # Premium Dark Mode Theme (Carbon, Midnight, Neon Cyan, Racing Red)
```

- **Language**: Kotlin 1.9.24
- **UI Framework**: Jetpack Compose with Material 3 (BOM 2024.05.00)
- **Architecture**: MVVM with Kotlin Coroutines and StateFlow
- **Location**: Google Play Services Fused Location Provider (`play-services-location:21.2.0`)
- **Database**: AndroidX Room 2.6.1 with KSP code generation
- **Map Engine**: MapLibre Native SDK (`org.maplibre.gl:android-sdk:11.0.0`)
- **Navigation**: AndroidX Navigation Compose 2.7.7
- **Target SDK**: 34 (Android 14) | **Min SDK**: 26 (Android 8.0) | **Compile SDK**: 34
- **JVM**: Java 17

---

## 🛠️ Build and Installation Instructions

### Prerequisites
- JDK 17 (e.g. OpenJDK 17 or Eclipse Temurin 17)
- Android SDK Platform 34 and Build-Tools 34.0.0

### Build Debug APK
Run the Gradle wrapper:
```bash
./gradlew assembleDebug
```
The output APK is located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install onto Device via ADB
Connect your Android phone with USB Debugging enabled:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

---

## 🗺️ Map Tile Provider Configuration

RoadPulse is configured by default with **OpenFreeMap Liberty**:
```kotlin
// Located in: app/src/main/java/com/roadpulse/app/ui/map/MapConfig.kt
const val DEFAULT_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
const val FALLBACK_STYLE_URL = "https://demotiles.maplibre.org/style.json"
```

- **No API key is required** for OpenFreeMap or Demotiles.
- To use custom or commercial map tile styles (such as MapTiler or Stadia), update `DEFAULT_STYLE_URL` in `MapConfig.kt` or provide your licensed style JSON URL.
- **Zero API keys are hardcoded in the repository**.

---

## 🔒 Permissions & Privacy

RoadPulse respects user privacy and never transmits telemetry to any external server:
- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`: Required for real-time GPS tracking.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION`: Required by Android 14 for background and screen-off trip recording.
- `POST_NOTIFICATIONS`: Required on Android 13+ to display the active trip tracking notification.
- `WAKE_LOCK`: Retains CPU execution state while phone is locked in a motorcycle mount or pocket.
- `INTERNET` and `ACCESS_NETWORK_STATE`: Used exclusively for fetching vector map tiles.

---

## 🧪 Verification & Feature Classification Table

| Feature / Requirement | Implementation Status | Automated Unit Test | Build Tested | Physical Device Required |
|---|---|---|---|---|
| Speedometer Gauge & Live km/h | Implemented | ✅ Yes | ✅ Yes (APK Built) | Real hardware GPS fixes |
| Geodesic Haversine Distance | Implemented | ✅ Passed (Paris/NY known pairs) | ✅ Yes | Real GPS hardware fixes |
| Speed Conversion (m/s ↔ km/h) | Implemented | ✅ Passed | ✅ Yes | Real GPS hardware fixes |
| Moving vs Overall Avg Speed | Implemented | ✅ Passed | ✅ Yes | Real GPS movement |
| GPS Quality Filter (Accuracy/Jitter) | Implemented | ✅ Passed (7 test cases) | ✅ Yes | Real GPS environment |
| Duplicate / Out-of-order Filter | Implemented | ✅ Passed | ✅ Yes | Real GPS stream |
| Impossible Jumps & Teleportation Filter | Implemented | ✅ Passed | ✅ Yes | Real GPS stream |
| Room Entity & DAO Storage | Implemented | ✅ Passed | ✅ Yes | Real device storage |
| Process Recreation & Trip Recovery | Implemented | ✅ Passed | ✅ Yes | Process kill test |
| Android 14 Location Foreground Service | Implemented | ✅ Passed | ✅ Yes | OS notification / screen-off check |
| WakeLock for Screen-Off Recording | Implemented | ✅ Passed | ✅ Yes | Physical screen-off test |
| MapLibre Polyline & Route Display | Implemented | N/A (UI Component) | ✅ Yes (Native libs packaged) | Device OpenGL rendering |
| Interactive Speed vs Distance Chart | Implemented | ✅ Passed (Downsampling/Edge cases) | ✅ Yes | Touch gesture interaction |
| GPX 1.1 XML Generation | Implemented | ✅ Passed (Format & Escaping) | ✅ Yes | Import into Garmin/Strava |
| CSV Metrics Generation | Implemented | ✅ Passed (Header & Row structure) | ✅ Yes | Open in Excel/Numbers |
| File Sharing / SAF Export | Implemented | ✅ Passed | ✅ Yes | Share sheet interaction |

---

## ⚠️ Known Limitations & Future Improvements
1. **Physical Device GPS**: Simulated automated tests confirm the mathematical and state-machine correctness. Verification of real-world multipath reflection, tunnels, and deep battery optimization (e.g. Samsung/Xiaomi OEM battery restrictions) requires real-world physical road testing.
2. **Offline Vector MBTiles**: MapLibre supports offline `.mbtiles` packages. In a future update, users can download regions directly to local storage for zero-data mountain exploration.
3. **Bluetooth LE Sensors**: Future integration with Bluetooth speed sensors and tire pressure monitors (TPMS).
