package com.roadpulse.app.domain.model

enum class GpsStatus(val label: String) {
    NO_PERMISSION("No Permission"),
    GPS_DISABLED("GPS Disabled"),
    SEARCHING("Searching GPS"),
    POOR_ACCURACY("Weak GPS"),
    ACCURATE("GPS Locked")
}
