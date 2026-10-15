# Rhody — Precision Motorcycle Telemetry, IMU Lean Angle & Parent Live Safety

[![Release](https://img.shields.io/github/v/release/NateshKannaR/Rhody?color=10B981&label=Release)](https://github.com/NateshKannaR/Rhody/releases/latest)
[![Download APK](https://img.shields.io/badge/Download-Android%20APK%20(v1.0.0)-2563EB?logo=android&logoColor=white)](https://github.com/NateshKannaR/Rhody/releases/download/v1.0.0/Rhody-v1.0.0.apk)
[![Cloud Web Cockpit](https://img.shields.io/badge/Web%20Cockpit-rhodyy.vercel.app-10B981)](https://rhodyy.vercel.app)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84?logo=android&logoColor=white)](https://github.com/NateshKannaR/Rhody)

**Rhody** is a native Android precision motorcycle telemetry computer and companion cloud telemetry platform. Engineered for sport riders, track-day racers, and touring enthusiasts, Rhody combines Android IMU sensor fusion (`TYPE_GYROSCOPE`, `TYPE_ACCELEROMETER`, `TYPE_GRAVITY`, `TYPE_ROTATION_VECTOR`) with high-frequency GPS tracking to measure real-time motorcycle **lean angle (degrees left/right)**, **lateral & longitudinal G-forces**, **sub-second sprint acceleration timers (0-60 & 0-100 km/h)**, and **Guardian Parent Live Tracking**.

---

## 📥 Download the App

| Package | Version | Direct Download | Release Details |
| :--- | :---: | :---: | :---: |
| **Rhody Android Mobile App (.apk)** | `v1.0.0` | [⬇️ **Download Rhody-v1.0.0.apk**](https://github.com/NateshKannaR/Rhody/releases/download/v1.0.0/Rhody-v1.0.0.apk) | [GitHub Releases](https://github.com/NateshKannaR/Rhody/releases/latest) |
| **Web Telemetry & Parent Cockpit** | Live Cloud | [🌐 **rhodyy.vercel.app**](https://rhodyy.vercel.app) | Real-time browser telemetry & Atlas sync |

```bash
# Quick Install via ADB
adb install -r Rhody-v1.0.0.apk
```

---

## 📱 Mobile App Screenshots

| Dashboard Cockpit & Speedometer | Live Ride & Parent Guardian Tracking | Performance Telemetry & Lean Angle |
| :---: | :---: | :---: |
| <img src="screenshots/dashboard.jpg" width="270" alt="Rhody Dashboard & Speedometer" /> | <img src="screenshots/live_trip.jpg" width="270" alt="Rhody Live Ride & Parent Safety Beacon" /> | <img src="screenshots/analytics.jpg" width="270" alt="Rhody Lean Angle & Performance Analytics" /> |
| Circular Speedometer, G-Force Radar & 0-60/0-100 Sprint Timers | Live Vector Map Route, Real-Time Velocity & Parent Safety Beacon | Lean Angle Cornering Gauge (42° L / 38° R) & Elevation Profile |

---

## 🚀 Key Features

### 1. Motorcycle Lean Angle & Cornering Telemetry
- **Hardware IMU Sensor Fusion**: Utilizes Android `SensorManager` with `Sensor.TYPE_ROTATION_VECTOR`, `Sensor.TYPE_GYROSCOPE`, `Sensor.TYPE_GRAVITY`, and `Sensor.TYPE_ACCELEROMETER`.
- **Dynamic Phone Mount Calibration**: Zero out baseline phone tilt with one tap in your motorcycle handlebar or tank mount.
- **Cornering Peak Hold**: Automatically captures maximum lean angle for both left and right turns (`42° L / 38° R`) with colored stability thresholds.

### 2. 🛡️ Guardian Parent Live Tracking
- **Peace of Mind for Parents**: Parents can watch the rider's progress in real-time on any phone or PC browser via `rhodyy.vercel.app`.
- **Live Status Heartbeat**: Displays whether the motorcycle is **MOVING** or **STOPPED**, current speed (km/h), live GPS pin, and battery status.
- **One-Click Share**: Riders can copy and share their live tracking link directly from the app.

### 3. Modern, Neat & Clean Native Android UI
- **Executive Slate & Cobalt Theme**: Glare-free dark UI designed for daylight readability on motorcycle mounts.
- **Dynamic Speedometer Gauge**: High-contrast circular analog/digital speedometer gauge with real-time speed in km/h (`mps * 3.6`), dynamic color transitions, acceleration arcs, and session peak speed indicators.
- **Modern Adaptive App Icon**: High-definition vector adaptive icon (`res/mipmap-anydpi-v26/ic_launcher.xml`) with dark titanium background, neon cyan pulse telemetry line, and electric velocity needle.

### 4. Advanced Performance Telemetry
- **G-Force Telemetry Meter**: Aviation crosshair radar widget displaying real-time lateral cornering Gs and longitudinal acceleration/braking Gs, backed by Android `SensorManager` (`Sensor.TYPE_LINEAR_ACCELERATION` / `Sensor.TYPE_ACCELEROMETER`) with GPS velocity derivative fallback.
- **Sprint Acceleration Timers (0–60 km/h & 0–100 km/h)**: Sub-second accurate standing launch detection with linear speed-time interpolation and false-pull abort filtering.
- **Elevation & Topography Tracking**: Altitudes recorded and plotted across every track point.

### 5. Companion Web Platform & MongoDB Atlas Integration
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

### 6. Robust Core Tracking Engine
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
Rhody
├── app/src/main/java/com/roadpulse/app/
│   ├── data
│   │   ├── database       # Room Entities (TripEntity, TrackPointEntity), DAOs, RoadPulseDatabase
│   │   ├── repository     # TripRepository, Coroutine Dispatchers, local transactions
│   │   └── sync           # CloudSyncManager (HTTP JSON sync to MongoDB Atlas API)
│   ├── domain
│   │   ├── calculator     # TripCalculator (Haversine math), SprintTimerCalculator (0-60, 0-100)
│   │   ├── filter         # GpsQualityFilter & FilterConfig (jump & jitter filtering)
│   │   └── model          # TripStatus, GpsStatus, GpsLocationFix, TripMetrics, GForceData, LeanAngleData
│   ├── service            # TripRecordingService (Foreground Service, WakeLock, IMU Sensor Fusion)
│   │                      # TripRecordingController (StateFlow event bus, singleton coordinator)
│   ├── export             # GpxExporter, CsvExporter, ExportManager (FileProvider)
│   └── ui
│       ├── components     # SpeedometerGauge, StatCard, GpsStatusBadge, GForceMeter, LeanAngleGauge, InteractiveSpeedChart, RouteMapView
│       ├── map            # MapConfig (OpenFreeMap style configuration)
│       ├── navigation     # Screen definitions, BottomBar
│       ├── screens        # Dashboard, LiveTrip, Analytics, History, TripDetail, Account
│       └── theme          # Slate Dark Theme (Slate, Cobalt Blue, Emerald Green, Amber, Rose)
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

Rhody supports unified user accounts powered by MongoDB Atlas:

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
- `HIGH_SAMPLING_RATE_SENSORS`: For high-frequency IMU gyroscope/accelerometer lean angle measurement.
- `POST_NOTIFICATIONS`: Android 13+ active trip tracking controls notification.
- `WAKE_LOCK`: Retains CPU execution state while phone is locked in a motorcycle mount or pocket.
- `INTERNET` and `ACCESS_NETWORK_STATE`: For vector map tiles and syncing with MongoDB Atlas.

---

## 📄 License
This project is licensed under the MIT License.

