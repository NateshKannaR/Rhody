# RoadPulse - Native Android Driving & Riding GPS Tracker & Web Telemetry Cloud

RoadPulse is a high-performance native Android GPS tracker and companion Web Telemetry platform engineered specifically for motorcycles, cars, and performance driving. Inspired by Enroad and Dragy, RoadPulse provides real-time GPS telemetry, an interactive speedometer, persistent background recording via an Android 14-compliant location foreground service, offline Room database storage, interactive vector map route tracking via MapLibre Native, high-resolution speed-versus-distance analysis charts, G-force telemetry, 0-60 / 0-100 km/h acceleration sprint timers, and seamless cloud synchronization with MongoDB Atlas.

---

## 🚀 Key Features

### 1. Modern, Neat & Clean Native Android UI
- **Aviation-Grade Dark Theme**: Carbon Dark (`#0D0F12`), Midnight Card (`#16191F`), Neon Cyan (`#00F5D4`), Racing Red (`#FF3366`), and Sport Amber (`#FFB703`).
- **Dynamic Speedometer Gauge**: High-contrast circular analog/digital speedometer gauge with real-time speed in km/h (`mps * 3.6`), dynamic color transitions, acceleration arcs, and session peak speed indicators.
- **Modern Adaptive App Icon**: High-definition vector adaptive icon (`res/mipmap-anydpi-v26/ic_launcher.xml`) with dark titanium background, neon cyan pulse telemetry line, and electric velocity needle.

### 2. Advanced Performance Telemetry
- **G-Force Telemetry Meter**: Aviation crosshair radar widget displaying real-time lateral cornering Gs and longitudinal acceleration/braking Gs, backed by Android `SensorManager` (`Sensor.TYPE_LINEAR_ACCELERATION` / `Sensor.TYPE_ACCELEROMETER`) with GPS velocity derivative fallback.
- **Sprint Acceleration Timers (0–60 km/h & 0–100 km/h)**: Sub-second accurate standing launch detection with linear speed-time interpolation and false-pull abort filtering.
- **Elevation & Topography Tracking**: Altitudes recorded and plotted across every track point.

### 3. Companion Web Platform & MongoDB Atlas Integration
- **Direct MongoDB Atlas Cloud Sync**: Integrated with MongoDB Atlas cluster:
  ```
  mongodb+srv://Natesh:Natesh@cluster0.wwp3oig.mongodb.net/roadpulse
  ```
- **Companion Web Dashboard (`backend/public/index.html`)**:
  - Interactive Leaflet dark-mode map with speed gradient polylines and scrubber point inspection.
  - Interactive Chart.js speed vs distance elevation profile with crosshair sync.
  - Standing-start sprint timer breakdown (0-60 km/h, 0-100 km/h).
  - Cloud trip manager, single-click GPX and CSV web exports, and live Atlas sync status.
- **One-Tap Cloud Sync from Android**:
  - Single-trip sync with live progress feedback in `TripDetailScreen`.
  - Batch "Sync All to MongoDB" in `HistoryScreen`.
  - Fully offline-first: app continues recording and saving to Room database even when offline.

### 4. Robust Core Tracking Engine
- **Android 14+ Foreground Service**:
  - Declared with `android:foregroundServiceType="location"`.
  - Immediate promotion to foreground in `onStartCommand` to prevent Android ANRs.
  - Acquires `PowerManager.PARTIAL_WAKE_LOCK` for continuous screen-off/pocket recording.
  - Rich notifications with live speed, distance, timer, and Pause/Resume/Finish controls.
  - Automatic trip recovery across app restarts.
- **GpsQualityFilter**:
  - Rejects fixes with horizontal accuracy > 25 meters.
  - Filters impossible teleportation jumps (> 250 km/h).
  - Eliminates stationary GPS drift at traffic lights or when parked (< 1.5m at < 0.5 m/s).
  - Sanitizes invalid coordinates, duplicate timestamps, and out-of-order time-travel fixes.
- **Interactive Vector Maps (MapLibre Native)**:
  - Vector tile rendering powered by OpenFreeMap Liberty (zero API keys required).
  - Camera auto-follow and manual recenter controls.
- **Interactive Speed-versus-Distance Graph**:
  - Touch-scrubbable Canvas chart with distance (km) on X-axis and speed (km/h) on Y-axis.
  - Peak speed indicator callouts and downsampled area fills.
- **Export & Sharing**:
  - **GPX 1.1 Export**: Compliant GPX XML with `<trk>`, `<trkseg>`, `<trkpt>`, elevation `<ele>`, ISO 8601 timestamps, and `<extensions>` (`<speed>`, `<accuracy>`).
  - **CSV Export**: Clean CSV with headers and per-point coordinates, speed, distance, and altitude.

---

## 🏗️ Architecture & Technology Stack

```
RoadPulse
├── app/src/main/java/com/roadpulse/app/
│   ├── data
│   │   ├── database       # Room Entities (TripEntity, TrackPointEntity), DAOs, RoadPulseDatabase
│   │   ├── repository     # TripRepository, Coroutine Dispatchers, local transactions
│   │   └── sync           # CloudSyncManager (HTTP JSON sync to MongoDB Atlas API)
│   ├── domain
│   │   ├── calculator     # TripCalculator (Haversine math), SprintTimerCalculator (0-60, 0-100)
│   │   ├── filter         # GpsQualityFilter & FilterConfig (jump & jitter filtering)
│   │   └── model          # TripStatus, GpsStatus, GpsLocationFix, TripMetrics, GForceData
│   ├── service            # TripRecordingService (Foreground Service, WakeLock, Sensors)
│   │                      # TripRecordingController (StateFlow event bus, singleton coordinator)
│   ├── export             # GpxExporter, CsvExporter, ExportManager (FileProvider)
│   └── ui
│       ├── components     # SpeedometerGauge, StatCard, GpsStatusBadge, GForceMeter, InteractiveSpeedChart, RouteMapView
│       ├── map            # MapConfig (OpenFreeMap style configuration)
│       ├── navigation     # Screen definitions, BottomBar
│       ├── screens        # Dashboard, LiveTrip, Analytics, History, TripDetail
│       └── theme          # Dark Theme (Carbon, Midnight, Neon Cyan, Racing Red, Sport Amber)
└── backend/               # Companion Web Service & MongoDB Atlas Backend
    ├── server.js          # Express.js API connected to MongoDB Atlas
    └── public/
        └── index.html     # High-tech Dark Mode Web Dashboard (Leaflet + Chart.js + Tailwind)
```

- **Android App**: Kotlin 1.9.24, Jetpack Compose (BOM 2024.05.00), Room 2.6.1, MapLibre Native 11.0.0, Coroutines & StateFlow.
- **Web Backend**: Node.js, Express.js, MongoDB Native Driver v6.12, Leaflet 1.9.4, Chart.js, Tailwind CSS.
- **Database**: MongoDB Atlas (`cluster0.wwp3oig.mongodb.net/roadpulse`) & Android Local SQLite/Room.

---

## 🛠️ Build and Running Instructions

### 1. Run the Companion Web Platform & MongoDB Backend
In the project root:
```bash
cd backend
npm install
npm start
```
The server will connect directly to MongoDB Atlas and serve the web dashboard on:
```
http://localhost:3000
```
Status endpoint:
```
http://localhost:3000/api/status
```

### 2. Build the Native Android APK
```bash
JAVA_HOME=/home/natesh/.tools/jdk-17 ANDROID_HOME=/home/natesh/android-sdk ./gradlew assembleDebug
```
The compiled, production-ready debug APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Run Unit Tests
```bash
JAVA_HOME=/home/natesh/.tools/jdk-17 ANDROID_HOME=/home/natesh/android-sdk ./gradlew testDebugUnitTest
```
Runs the automated test suite including:
- Geodesic Haversine distance tests
- Speed conversions (m/s ↔ km/h)
- Moving vs overall average speeds
- GPS quality filter jitter and jump filtering (7 test scenarios)
- Trip state machine lifecycle and persistence
- GPX 1.1 and CSV export formatting
- SprintTimerCalculator 0-60 and 0-100 km/h standing launch detection

### 4. Install onto Phone via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔐 Email ID Authentication & MongoDB Atlas Cloud

RoadPulse supports unified user accounts powered by MongoDB Atlas:

### Features & Security
- **Email ID Registration & Login**: Drivers and riders can sign up with their email and password.
- **Salted Hash Security**: Passwords are securely hashed with per-user cryptographic salts using Node.js `pbkdf2Sync` (SHA-512, 1000 iterations).
- **HMAC Session Tokens**: Cryptographically signed bearer tokens authenticate API operations and tie telemetry records to specific user profiles.
- **User-Filtered Cloud Trips**: When logged in, both the Android app and the Web Dashboard query and synchronize trips specific to the logged-in email.
- **Native Android Account Screen**:
  - Accessible via the Account tab (bottom navigation) or the top header profile pill.
  - One-tap toggle between Sign In and Account Creation.
  - Active MongoDB Atlas connection indicator.
  - One-tap "Sync All Local Trips Now" pushing Room database trips to MongoDB Atlas.
  - Configurable backend server gateway endpoint.
- **Web Dashboard Auth Modal**:
  - Sleek dark-mode modal for instant sign-in or account registration.
  - Local session storage with real-time reactive UI update.
  - Live simulation & upload tied directly to the logged-in email.

### Authentication Endpoints
- `POST /api/auth/register`: Create account (`{ email, password, name }`)
- `POST /api/auth/login`: Authenticate (`{ email, password }`)
- `GET /api/auth/me`: Get current authenticated user profile (`Authorization: Bearer <token>`)
- `GET /api/trips?email=<user@example.com>`: Fetch trips associated with user
- `POST /api/trips/sync`: Sync trip and GPS points associated with user

---

## 🔒 Permissions & Security
- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`: Required for sub-second GPS telemetry.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION`: Required by Android 14 for uninterrupted background tracking.
- `POST_NOTIFICATIONS`: Android 13+ active trip tracking controls notification.
- `WAKE_LOCK`: Retains CPU execution state while phone is locked in a motorcycle mount or pocket.
- `INTERNET` and `ACCESS_NETWORK_STATE`: For vector map tiles and syncing with MongoDB Atlas.

