package com.roadpulse.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.roadpulse.app.R
import com.roadpulse.app.RoadPulseApp
import com.roadpulse.app.data.database.RoadPulseDatabase
import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.data.repository.TripRepository
import com.roadpulse.app.data.sync.CloudSyncManager
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.domain.filter.FilterConfig
import com.roadpulse.app.domain.filter.GpsQualityFilter
import com.roadpulse.app.domain.model.GpsLocationFix
import com.roadpulse.app.domain.model.GpsStatus
import com.roadpulse.app.domain.model.TripMetrics
import com.roadpulse.app.domain.model.TripStatus
import com.roadpulse.app.ui.MainActivity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import com.roadpulse.app.domain.model.GForceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class TripRecordingService : LifecycleService() {

    companion object {
        var isServiceRunning: Boolean = false
            private set
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: TripRepository
    private var wakeLock: PowerManager.WakeLock? = null
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private var peakG: Float = 0f

    // High-performance track point buffer (averts individual 1Hz disk transactions)
    private val pointBuffer = ConcurrentLinkedQueue<TrackPointEntity>()
    private var lastBatchFlushMs: Long = 0L
    private val isFlushingPoints = AtomicBoolean(false)

    // Robust live beacon concurrency guard & adaptive failure backoff
    private val isBeaconBroadcasting = AtomicBoolean(false)
    private var lastBeaconAttemptMs: Long = 0L
    private var consecutiveBeaconFailures: Int = 0

    private val qualityFilter = GpsQualityFilter(FilterConfig())
    private var previousAcceptedFix: GpsLocationFix? = null

    private var activeTripId: Long = 0L
    private var tripStatus: TripStatus = TripStatus.IDLE

    private var currentMetrics = TripMetrics()
    private var tickerJob: Job? = null
    private var tripStartTime: Long = 0L
    private var pauseStartTime: Long = 0L
    private var totalPausedMs: Long = 0L
    private var movingDurationMs: Long = 0L
    private var cumulativeDistanceMeters: Double = 0.0
    private var maxSpeedMps: Float = 0f
    private var lastSensorUpdateMs: Long = 0L

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null) return
            val now = System.currentTimeMillis()
            // Cap at 4 Hz (250ms) to conserve CPU/battery and eliminate main looper starvation
            if (now - lastSensorUpdateMs < 250L) return
            lastSensorUpdateMs = now

            val xG = event.values[0] / 9.80665f
            val yG = event.values[1] / 9.80665f
            val totalG = kotlin.math.sqrt(xG * xG + yG * yG)
            if (totalG > peakG) {
                peakG = totalG
            }
            TripRecordingController.updateGForce(
                GForceData(
                    lateralG = xG,
                    longitudinalG = yG,
                    maxG = peakG
                )
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleNewLocation(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        repository = TripRepository(RoadPulseDatabase.getInstance(this))
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.action ?: TripRecordingController.ACTION_START

        // Immediately promote to foreground to comply with Android 14+ 5-second deadline
        promoteToForeground()

        when (action) {
            TripRecordingController.ACTION_START -> handleStart()
            TripRecordingController.ACTION_PAUSE -> handlePause()
            TripRecordingController.ACTION_RESUME -> handleResume()
            TripRecordingController.ACTION_FINISH -> handleFinish()
        }

        return START_STICKY
    }

    private fun promoteToForeground() {
        val notification = buildNotification(currentMetrics)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                RoadPulseApp.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(RoadPulseApp.NOTIFICATION_ID, notification)
        }
    }

    private fun handleStart() {
        if (tripStatus == TripStatus.RECORDING) return

        lifecycleScope.launch {
            if (activeTripId == 0L) {
                // Check if recovering an unfinished trip
                val existing = repository.getActiveOrPausedTrip()
                if (existing != null) {
                    activeTripId = existing.id
                    tripStartTime = existing.startTime
                    cumulativeDistanceMeters = existing.totalDistanceMeters
                    maxSpeedMps = existing.maxSpeedMps
                    movingDurationMs = existing.movingDurationMs
                    totalPausedMs = existing.pauseDurationMs

                    val points = repository.getRecentPoints(existing.id, 200)
                    TripRecordingController.setLivePoints(points)
                    previousAcceptedFix = points.lastOrNull()?.let {
                        GpsLocationFix(
                            timestamp = it.timestamp,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            speedMps = it.speedMps,
                            accuracyMeters = it.accuracyMeters,
                            altitudeMeters = it.altitudeMeters,
                            bearingDegrees = it.bearingDegrees,
                            hasValidSpeed = true
                        )
                    }
                } else {
                    tripStartTime = System.currentTimeMillis()
                    activeTripId = repository.createTrip(tripStartTime)
                    TripRecordingController.clearLivePoints()
                    cumulativeDistanceMeters = 0.0
                    maxSpeedMps = 0f
                    movingDurationMs = 0L
                    totalPausedMs = 0L
                }
            }

            tripStatus = TripStatus.RECORDING
            startLocationUpdates()
            startTicker()
            updateStateAndNotification()
        }
    }

    private fun handlePause() {
        if (tripStatus != TripStatus.RECORDING) return

        tripStatus = TripStatus.PAUSED
        pauseStartTime = System.currentTimeMillis()
        stopLocationUpdates()
        stopTicker()
        flushPointBuffer(forceAll = true)

        currentMetrics = currentMetrics.copy(
            tripStatus = TripStatus.PAUSED,
            currentSpeedKmh = 0f
        )
        TripRecordingController.updateMetrics(currentMetrics)
        promoteToForeground()

        lifecycleScope.launch(Dispatchers.IO) {
            repository.updateTripProgress(activeTripId, currentMetrics, TripStatus.PAUSED)
        }
    }

    private fun handleResume() {
        if (tripStatus != TripStatus.PAUSED) return

        val now = System.currentTimeMillis()
        if (pauseStartTime > 0L) {
            totalPausedMs += (now - pauseStartTime)
            pauseStartTime = 0L
        }

        tripStatus = TripStatus.RECORDING
        startLocationUpdates()
        startTicker()
        updateStateAndNotification()

        lifecycleScope.launch(Dispatchers.IO) {
            repository.updateTripProgress(activeTripId, currentMetrics, TripStatus.RECORDING)
        }
    }

    private fun handleFinish() {
        stopLocationUpdates()
        stopTicker()
        flushPointBuffer(forceAll = true)
        tripStatus = TripStatus.FINISHED

        lifecycleScope.launch(Dispatchers.IO) {
            if (activeTripId == 0L) {
                val orphan = repository.getActiveOrPausedTrip()
                if (orphan != null) {
                    activeTripId = orphan.id
                    if (cumulativeDistanceMeters <= 0.0) {
                        cumulativeDistanceMeters = orphan.totalDistanceMeters
                    }
                }
            }

            if (activeTripId > 0L) {
                // Failsafe for 0 KM: recalculate from recorded points if distance is 0
                if (cumulativeDistanceMeters <= 0.0) {
                    val points = repository.getPoints(activeTripId)
                    var fallbackDist = 0.0
                    for (i in 1 until points.size) {
                        fallbackDist += TripCalculator.calculateDistanceMeters(
                            points[i - 1].latitude, points[i - 1].longitude,
                            points[i].latitude, points[i].longitude
                        )
                    }
                    if (fallbackDist > 0.0) {
                        cumulativeDistanceMeters = fallbackDist
                    }
                }

                val finalMetrics = currentMetrics.copy(
                    tripStatus = TripStatus.FINISHED,
                    currentSpeedKmh = 0f,
                    totalDistanceMeters = cumulativeDistanceMeters
                )
                repository.finishTrip(activeTripId, finalMetrics)

                // Inform parents ride has concluded safely
                try {
                    CloudSyncManager.broadcastLiveBeacon(
                        tripId = activeTripId,
                        metrics = finalMetrics,
                        isMoving = false,
                        batteryPercent = getBatteryPercentage()
                    )
                } catch (_: Exception) {}
            }

            // Return controller metrics to IDLE immediately so UI displays 'START RECORDING RIDE'
            TripRecordingController.updateMetrics(TripMetrics(tripStatus = TripStatus.IDLE))
            TripRecordingController.clearLivePoints()

            releaseWakeLock()
            ServiceCompat.stopForeground(this@TripRecordingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(0f)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            if (accelerometer != null && sensorThread == null) {
                val thread = HandlerThread("RhodySensorThread").apply { start() }
                sensorThread = thread
                val handler = Handler(thread.looper)
                sensorHandler = handler
                sensorManager?.registerListener(
                    sensorListener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    handler
                )
            }
        } catch (e: SecurityException) {
            TripRecordingController.updateGpsStatus(GpsStatus.NO_PERMISSION)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager?.unregisterListener(sensorListener)
        try {
            sensorThread?.quitSafely()
        } catch (_: Exception) {}
        sensorThread = null
        sensorHandler = null
    }

    private fun handleNewLocation(location: Location) {
        val hasSpeed = location.hasSpeed() && location.speed >= 0f
        val rawSpeedMps = if (hasSpeed) location.speed else 0f

        val incomingFix = GpsLocationFix(
            timestamp = location.time,
            latitude = location.latitude,
            longitude = location.longitude,
            speedMps = rawSpeedMps,
            accuracyMeters = if (location.hasAccuracy()) location.accuracy else 99f,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
            bearingDegrees = if (location.hasBearing()) location.bearing else null,
            hasValidSpeed = hasSpeed
        )

        // Evaluate location through quality filter
        val filterResult = qualityFilter.evaluate(
            incomingFix = incomingFix,
            previousAcceptedFix = previousAcceptedFix,
            currentTimeMs = System.currentTimeMillis()
        )

        if (!filterResult.accepted) {
            // Update GPS status if accuracy was poor
            if (incomingFix.accuracyMeters > 25f) {
                TripRecordingController.updateGpsStatus(GpsStatus.POOR_ACCURACY)
            }
            return
        }

        val fix = filterResult.sanitizedFix ?: incomingFix
        val gpsStatus = if (fix.accuracyMeters <= 12f) GpsStatus.ACCURATE else GpsStatus.POOR_ACCURACY

        var currentSpeedMps = fix.speedMps
        if (tripStatus == TripStatus.RECORDING && previousAcceptedFix != null && !filterResult.isStationaryNoise) {
            val dMeters = TripCalculator.calculateDistanceMeters(
                previousAcceptedFix!!.latitude,
                previousAcceptedFix!!.longitude,
                fix.latitude,
                fix.longitude
            )

            // If device didn't report valid speed, derive it from distance & time delta
            if (!fix.hasValidSpeed) {
                val dtSec = (fix.timestamp - previousAcceptedFix!!.timestamp) / 1000.0
                if (dtSec > 0.0) {
                    currentSpeedMps = (dMeters / dtSec).toFloat()
                }
            }

            cumulativeDistanceMeters += dMeters
            if (currentSpeedMps > maxSpeedMps) {
                maxSpeedMps = currentSpeedMps
            }
        }

        val currentSpeedKmh = TripCalculator.mpsToKmh(currentSpeedMps)

        if (accelerometer == null && previousAcceptedFix != null) {
            val dtSec = ((fix.timestamp - previousAcceptedFix!!.timestamp) / 1000f).coerceAtLeast(0.1f)
            val gpsLongG = ((currentSpeedMps - previousAcceptedFix!!.speedMps) / dtSec) / 9.80665f
            val mag = kotlin.math.abs(gpsLongG)
            if (mag > peakG) peakG = mag
            TripRecordingController.updateGForce(
                GForceData(
                    lateralG = 0f,
                    longitudinalG = gpsLongG,
                    maxG = peakG
                )
            )
        }

        // Buffered Point persistence (flushed in batches to minimize flash disk transactions)
        if (tripStatus == TripStatus.RECORDING && activeTripId > 0L) {
            val pointEntity = TrackPointEntity(
                tripId = activeTripId,
                timestamp = fix.timestamp,
                latitude = fix.latitude,
                longitude = fix.longitude,
                speedMps = currentSpeedMps,
                accuracyMeters = fix.accuracyMeters,
                altitudeMeters = fix.altitudeMeters,
                cumulativeDistanceMeters = cumulativeDistanceMeters,
                bearingDegrees = fix.bearingDegrees
            )

            pointBuffer.offer(pointEntity)
            TripRecordingController.addLivePoint(pointEntity)

            val nowMs = System.currentTimeMillis()
            if (pointBuffer.size >= 5 || (nowMs - lastBatchFlushMs >= 5000L && pointBuffer.isNotEmpty())) {
                flushPointBuffer()
            }
        }

        // Update in-memory metrics
        val now = System.currentTimeMillis()
        val totalElapsedMs = (now - tripStartTime - totalPausedMs).coerceAtLeast(0L)
        val avgMovingKmh = TripCalculator.calculateMovingAverageSpeedKmh(cumulativeDistanceMeters, movingDurationMs)
        val avgOverallKmh = TripCalculator.calculateOverallAverageSpeedKmh(cumulativeDistanceMeters, totalElapsedMs)

        currentMetrics = currentMetrics.copy(
            currentSpeedKmh = currentSpeedKmh,
            maxSpeedKmh = TripCalculator.mpsToKmh(maxSpeedMps),
            avgMovingSpeedKmh = avgMovingKmh,
            avgOverallSpeedKmh = avgOverallKmh,
            totalDistanceMeters = cumulativeDistanceMeters,
            elapsedTimeMs = totalElapsedMs,
            movingDurationMs = movingDurationMs,
            pauseDurationMs = totalPausedMs,
            accuracyMeters = fix.accuracyMeters,
            gpsStatus = gpsStatus,
            activeTripId = activeTripId,
            tripStatus = tripStatus,
            currentLatitude = fix.latitude,
            currentLongitude = fix.longitude
        )

        TripRecordingController.updateMetrics(currentMetrics)
        TripRecordingController.updateFix(fix)
        previousAcceptedFix = fix
    }

    private fun flushPointBuffer(forceAll: Boolean = false) {
        if (!isFlushingPoints.compareAndSet(false, true)) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val batch = ArrayList<TrackPointEntity>()
                val maxDrain = if (forceAll) Int.MAX_VALUE else 50
                while (batch.size < maxDrain) {
                    val pt = pointBuffer.poll() ?: break
                    batch.add(pt)
                }
                if (batch.isNotEmpty()) {
                    repository.recordTrackPointsBatch(batch)
                    lastBatchFlushMs = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                // If disk was momentarily busy, points remain safely in memory for next cycle
            } finally {
                isFlushingPoints.set(false)
                if (pointBuffer.size >= 5 || (forceAll && pointBuffer.isNotEmpty())) {
                    flushPointBuffer(forceAll)
                }
            }
        }
    }

    private fun getBatteryPercentage(): Int? {
        return try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (_: Exception) {
            null
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = lifecycleScope.launch {
            var counter = 0
            while (isActive) {
                delay(1000L)
                if (tripStatus == TripStatus.RECORDING) {
                    val now = System.currentTimeMillis()
                    val totalElapsedMs = (now - tripStartTime - totalPausedMs).coerceAtLeast(0L)

                    // Increment moving duration if moving > 1.8 km/h
                    if (currentMetrics.currentSpeedKmh >= TripCalculator.MOVING_SPEED_THRESHOLD_KMH) {
                        movingDurationMs += 1000L
                    }

                    val avgMovingKmh = TripCalculator.calculateMovingAverageSpeedKmh(cumulativeDistanceMeters, movingDurationMs)
                    val avgOverallKmh = TripCalculator.calculateOverallAverageSpeedKmh(cumulativeDistanceMeters, totalElapsedMs)

                    currentMetrics = currentMetrics.copy(
                        elapsedTimeMs = totalElapsedMs,
                        movingDurationMs = movingDurationMs,
                        pauseDurationMs = totalPausedMs,
                        avgMovingSpeedKmh = avgMovingKmh,
                        avgOverallSpeedKmh = avgOverallKmh
                    )
                    TripRecordingController.updateMetrics(currentMetrics)

                    counter++

                    // Flush any pending buffered track points periodically every 5 seconds
                    if (counter % 5 == 0 && pointBuffer.isNotEmpty()) {
                        flushPointBuffer()
                    }

                    // Update notification smoothly every 5 seconds (not 3 seconds)
                    if (counter % 5 == 0) {
                        updateNotificationOnly()
                    }

                    // Persist trip metadata summary to DB every 30 seconds to avoid Room invalidation storms
                    if (counter % 30 == 0 && activeTripId > 0L) {
                        val snapshot = currentMetrics.copy()
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                repository.updateTripProgress(activeTripId, snapshot, tripStatus)
                            } catch (_: Exception) {}
                        }
                    }

                    // Broadcast live telemetry beacon with atomic in-flight guard & adaptive failure backoff
                    val nowMs = System.currentTimeMillis()
                    val beaconInterval = when {
                        consecutiveBeaconFailures >= 5 -> 60_000L
                        consecutiveBeaconFailures >= 2 -> 30_000L
                        else -> 10_000L
                    }

                    if (activeTripId > 0L && (nowMs - lastBeaconAttemptMs >= beaconInterval)) {
                        if (isBeaconBroadcasting.compareAndSet(false, true)) {
                            lastBeaconAttemptMs = nowMs
                            val isMoving = currentMetrics.currentSpeedKmh >= TripCalculator.MOVING_SPEED_THRESHOLD_KMH
                            val snapshotMetrics = currentMetrics.copy()
                            val battery = getBatteryPercentage()

                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val res = CloudSyncManager.broadcastLiveBeacon(
                                        tripId = activeTripId,
                                        metrics = snapshotMetrics,
                                        isMoving = isMoving,
                                        batteryPercent = battery
                                    )
                                    if (res.isSuccess) {
                                        consecutiveBeaconFailures = 0
                                    } else {
                                        consecutiveBeaconFailures++
                                    }
                                } catch (_: Exception) {
                                    consecutiveBeaconFailures++
                                } finally {
                                    isBeaconBroadcasting.set(false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateStateAndNotification() {
        TripRecordingController.updateMetrics(currentMetrics)
        updateNotificationOnly()
    }

    private fun updateNotificationOnly() {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(RoadPulseApp.NOTIFICATION_ID, buildNotification(currentMetrics))
    }

    private fun buildNotification(metrics: TripMetrics): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val speedStr = String.format("%.0f km/h", metrics.currentSpeedKmh)
        val distStr = TripCalculator.formatDistance(metrics.totalDistanceMeters)
        val timeStr = TripCalculator.formatDuration(metrics.elapsedTimeMs)

        val title = when (tripStatus) {
            TripStatus.RECORDING -> "Rhody Tracking - $speedStr"
            TripStatus.PAUSED -> "Rhody - Trip Paused"
            else -> "Rhody Trip"
        }

        val content = "$distStr | $timeStr | Max: ${String.format("%.0f", metrics.maxSpeedKmh)} km/h"

        val builder = NotificationCompat.Builder(this, RoadPulseApp.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(contentIntent)
            .setOngoing(tripStatus == TripStatus.RECORDING)
            .setOnlyAlertOnce(true)

        // Actions
        if (tripStatus == TripStatus.RECORDING) {
            val pauseIntent = PendingIntent.getService(
                this,
                1,
                Intent(this, TripRecordingService::class.java).apply { action = TripRecordingController.ACTION_PAUSE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
        } else if (tripStatus == TripStatus.PAUSED) {
            val resumeIntent = PendingIntent.getService(
                this,
                2,
                Intent(this, TripRecordingService::class.java).apply { action = TripRecordingController.ACTION_RESUME },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumeIntent)
        }

        val finishIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, TripRecordingService::class.java).apply { action = TripRecordingController.ACTION_FINISH },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Finish", finishIntent)

        return builder.build()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoadPulse:RecordingWakeLock").apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24-hour safety cap
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    override fun onDestroy() {
        isServiceRunning = false
        flushPointBuffer(forceAll = true)
        stopLocationUpdates()
        stopTicker()
        releaseWakeLock()
        super.onDestroy()
    }
}
