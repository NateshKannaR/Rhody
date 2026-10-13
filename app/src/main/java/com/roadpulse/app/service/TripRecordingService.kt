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
import com.roadpulse.app.domain.calculator.TripCalculator
import com.roadpulse.app.domain.filter.FilterConfig
import com.roadpulse.app.domain.filter.GpsQualityFilter
import com.roadpulse.app.domain.model.GpsLocationFix
import com.roadpulse.app.domain.model.GpsStatus
import com.roadpulse.app.domain.model.TripMetrics
import com.roadpulse.app.domain.model.TripStatus
import com.roadpulse.app.ui.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TripRecordingService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: TripRepository
    private var wakeLock: PowerManager.WakeLock? = null

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

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleNewLocation(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = TripRepository(RoadPulseDatabase.getInstance(this))
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

                    val points = repository.getPoints(existing.id)
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

        currentMetrics = currentMetrics.copy(
            tripStatus = TripStatus.PAUSED,
            currentSpeedKmh = 0f
        )
        TripRecordingController.updateMetrics(currentMetrics)
        promoteToForeground()

        lifecycleScope.launch {
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

        lifecycleScope.launch {
            repository.updateTripProgress(activeTripId, currentMetrics, TripStatus.RECORDING)
        }
    }

    private fun handleFinish() {
        stopLocationUpdates()
        stopTicker()
        tripStatus = TripStatus.FINISHED

        val finalMetrics = currentMetrics.copy(
            tripStatus = TripStatus.FINISHED,
            currentSpeedKmh = 0f
        )
        TripRecordingController.updateMetrics(finalMetrics)

        lifecycleScope.launch {
            if (activeTripId > 0L) {
                repository.finishTrip(activeTripId, finalMetrics)
            }
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
        } catch (e: SecurityException) {
            TripRecordingController.updateGpsStatus(GpsStatus.NO_PERMISSION)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
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

        previousAcceptedFix = fix
        val currentSpeedKmh = TripCalculator.mpsToKmh(currentSpeedMps)

        // Point persistence
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

            lifecycleScope.launch {
                repository.recordTrackPoint(pointEntity)
            }
            TripRecordingController.addLivePoint(pointEntity)
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

                    // Periodically update notification and persist to database every 3 seconds
                    counter++
                    if (counter % 3 == 0) {
                        updateNotificationOnly()
                        repository.updateTripProgress(activeTripId, currentMetrics, tripStatus)
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
            TripStatus.RECORDING -> "RoadPulse Tracking - $speedStr"
            TripStatus.PAUSED -> "RoadPulse - Trip Paused"
            else -> "RoadPulse Trip"
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
        stopLocationUpdates()
        stopTicker()
        releaseWakeLock()
        super.onDestroy()
    }
}
