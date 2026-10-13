package com.roadpulse.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.roadpulse.app.di.AppContainer

class RoadPulseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RoadPulse Active Trip Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows persistent tracking metrics and status during active trip recording."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "roadpulse_trip_recording_channel"
        const val NOTIFICATION_ID = 1001
    }
}
