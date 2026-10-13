package com.roadpulse.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.domain.model.GpsLocationFix
import com.roadpulse.app.domain.model.GpsStatus
import com.roadpulse.app.domain.model.TripMetrics
import com.roadpulse.app.domain.model.TripStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TripRecordingController {
    const val ACTION_START = "com.roadpulse.app.action.START"
    const val ACTION_PAUSE = "com.roadpulse.app.action.PAUSE"
    const val ACTION_RESUME = "com.roadpulse.app.action.RESUME"
    const val ACTION_FINISH = "com.roadpulse.app.action.FINISH"

    private val _metrics = MutableStateFlow(TripMetrics())
    val metrics: StateFlow<TripMetrics> = _metrics.asStateFlow()

    private val _currentFix = MutableStateFlow<GpsLocationFix?>(null)
    val currentFix: StateFlow<GpsLocationFix?> = _currentFix.asStateFlow()

    private val _livePoints = MutableStateFlow<List<TrackPointEntity>>(emptyList())
    val livePoints: StateFlow<List<TrackPointEntity>> = _livePoints.asStateFlow()

    private val _liveGForce = MutableStateFlow(com.roadpulse.app.domain.model.GForceData())
    val liveGForce: StateFlow<com.roadpulse.app.domain.model.GForceData> = _liveGForce.asStateFlow()

    fun updateGForce(gForce: com.roadpulse.app.domain.model.GForceData) {
        _liveGForce.value = gForce
    }

    fun updateMetrics(newMetrics: TripMetrics) {
        _metrics.value = newMetrics
    }

    fun updateFix(fix: GpsLocationFix?) {
        _currentFix.value = fix
    }

    fun updateGpsStatus(status: GpsStatus) {
        _metrics.value = _metrics.value.copy(gpsStatus = status)
    }

    fun addLivePoint(point: TrackPointEntity) {
        val currentList = _livePoints.value
        _livePoints.value = currentList + point
    }

    fun setLivePoints(points: List<TrackPointEntity>) {
        _livePoints.value = points
    }

    fun clearLivePoints() {
        _livePoints.value = emptyList()
    }

    fun startTrip(context: Context) {
        val intent = Intent(context, TripRecordingService::class.java).apply {
            action = ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun pauseTrip(context: Context) {
        val intent = Intent(context, TripRecordingService::class.java).apply {
            action = ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeTrip(context: Context) {
        val intent = Intent(context, TripRecordingService::class.java).apply {
            action = ACTION_RESUME
        }
        context.startService(intent)
    }

    fun finishTrip(context: Context) {
        val intent = Intent(context, TripRecordingService::class.java).apply {
            action = ACTION_FINISH
        }
        context.startService(intent)
    }
}
