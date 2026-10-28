package com.roadpulse.app.data.sync

import android.content.Context
import com.roadpulse.app.data.database.TrackPointEntity
import com.roadpulse.app.data.database.TripEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object CloudSyncManager {
    private const val PREFS_NAME = "roadpulse_sync_prefs"
    private const val KEY_SERVER_URL = "server_base_url"

    const val DEFAULT_SERVER_URL = "https://rhodyy.vercel.app"

    var serverBaseUrl: String = DEFAULT_SERVER_URL
        private set

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SERVER_URL, null)
        serverBaseUrl = if (saved.isNullOrEmpty() || saved.contains("10.0.2.2")) {
            DEFAULT_SERVER_URL
        } else {
            saved
        }
    }

    fun setServerUrl(context: Context, newUrl: String) {
        serverBaseUrl = newUrl.trim().trimEnd('/')
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_URL, serverBaseUrl).apply()
    }

    suspend fun checkServerHealth(): Result<JSONObject> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$serverBaseUrl/api/status")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                Result.success(JSONObject(responseText))
            } else {
                Result.failure(Exception("HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun syncTripToMongo(
        trip: TripEntity,
        points: List<TrackPointEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$serverBaseUrl/api/trips/sync")
            val authState = com.roadpulse.app.data.auth.AuthManager.authState.value
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (!authState.token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer ${authState.token}")
                }
                connectTimeout = 8000
                readTimeout = 15000
                doOutput = true
                doInput = true
            }

            // Build Trip JSON
            val tripJson = JSONObject().apply {
                put("id", trip.id)
                put("title", trip.title)
                put("notes", trip.notes ?: "")
                put("startTime", trip.startTime)
                put("endTime", trip.endTime ?: 0L)
                put("status", trip.status)
                put("totalDistanceMeters", trip.totalDistanceMeters)
                put("maxSpeedMps", trip.maxSpeedMps.toDouble())
                put("avgMovingSpeedMps", trip.avgMovingSpeedMps.toDouble())
                put("avgOverallSpeedMps", trip.avgOverallSpeedMps.toDouble())
                put("movingDurationMs", trip.movingDurationMs)
                put("totalDurationMs", trip.totalDurationMs)
                put("pauseDurationMs", trip.pauseDurationMs)
                put("createdAt", trip.createdAt)
                if (authState.user?.email != null) {
                    put("userEmail", authState.user.email)
                }
            }

            // Build Points JSON Array
            val pointsArray = JSONArray()
            for (p in points) {
                val pJson = JSONObject().apply {
                    put("id", p.id)
                    put("tripId", p.tripId)
                    put("timestamp", p.timestamp)
                    put("latitude", p.latitude)
                    put("longitude", p.longitude)
                    put("speedMps", p.speedMps.toDouble())
                    put("accuracyMeters", p.accuracyMeters.toDouble())
                    if (p.altitudeMeters != null) put("altitudeMeters", p.altitudeMeters)
                    put("cumulativeDistanceMeters", p.cumulativeDistanceMeters)
                    if (p.bearingDegrees != null) put("bearingDegrees", p.bearingDegrees.toDouble())
                }
                pointsArray.put(pJson)
            }

            val payload = JSONObject().apply {
                put("trip", tripJson)
                put("points", pointsArray)
                if (authState.user?.email != null) {
                    put("userEmail", authState.user.email)
                }
            }

            // Send payload
            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val msg = try {
                    JSONObject(responseText).optString("message", "Synced to Rhody Cloud!")
                } catch (_: Exception) {
                    "Synced to Rhody Cloud! ($responseCode)"
                }
                Result.success(msg)
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $responseCode"
                Result.failure(Exception("HTTP $responseCode: $errorText"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Sync failed: ${e.localizedMessage}"))
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun broadcastLiveBeacon(
        tripId: Long,
        metrics: com.roadpulse.app.domain.model.TripMetrics,
        isMoving: Boolean,
        batteryPercent: Int?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$serverBaseUrl/api/trips/$tripId/live")
            val authState = com.roadpulse.app.data.auth.AuthManager.authState.value
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (!authState.token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer ${authState.token}")
                }
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
                doInput = true
            }

            val payload = JSONObject().apply {
                put("riderName", authState.user?.name ?: authState.user?.email?.substringBefore("@") ?: "Rider")
                put("status", metrics.tripStatus.name)
                put("isMoving", isMoving)
                put("currentSpeedKmh", metrics.currentSpeedKmh.toDouble())
                put("maxSpeedKmh", metrics.maxSpeedKmh.toDouble())
                put("latitude", metrics.currentLatitude ?: 0.0)
                put("longitude", metrics.currentLongitude ?: 0.0)
                if (batteryPercent != null) put("batteryPercent", batteryPercent)
                put("totalDistanceKm", metrics.totalDistanceMeters / 1000.0)
                put("elapsedTimeMs", metrics.elapsedTimeMs)
                put("timestamp", System.currentTimeMillis())
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun fetchUserProfileAndBike(context: Context): Result<JSONObject> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val authState = com.roadpulse.app.data.auth.AuthManager.authState.value
            val url = URL("$serverBaseUrl/api/user/profile")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                if (!authState.token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer ${authState.token}")
                }
                connectTimeout = 6000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseText)
                val profile = root.optJSONObject("profile")
                if (profile != null) {
                    val bikeModel = profile.optString("bikeModel", "")
                    val bikeSpecs = profile.optString("bikeSpecs", "")
                    val bikePlate = profile.optString("bikePlate", "")

                    val bikePrefs = context.applicationContext.getSharedPreferences("rhody_bike_prefs", Context.MODE_PRIVATE)
                    val editor = bikePrefs.edit()
                    if (bikeModel.isNotBlank()) {
                        editor.putString("bike_model", bikeModel)
                        editor.putBoolean("is_configured", true)
                    }
                    if (bikeSpecs.isNotBlank()) editor.putString("bike_specs", bikeSpecs)
                    if (bikePlate.isNotBlank()) editor.putString("bike_plate", bikePlate)
                    editor.apply()
                }
                Result.success(root)
            } else {
                Result.failure(Exception("HTTP $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun saveUserProfileAndBike(
        context: Context,
        name: String? = null,
        bikeModel: String,
        bikePlate: String,
        bikeSpecs: String,
        bikeOdo: String,
        emergencyContactName: String? = null,
        emergencyContactPhone: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var profileConn: HttpURLConnection? = null
        var bikeConn: HttpURLConnection? = null
        try {
            val authState = com.roadpulse.app.data.auth.AuthManager.authState.value

            // 1. Save to /api/user/profile
            val profileUrl = URL("$serverBaseUrl/api/user/profile")
            profileConn = (profileUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (!authState.token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer ${authState.token}")
                }
                connectTimeout = 8000
                readTimeout = 12000
                doOutput = true
                doInput = true
            }

            val profilePayload = JSONObject().apply {
                if (name != null) put("name", name)
                put("bikeModel", bikeModel)
                put("bikePlate", bikePlate)
                put("bikeSpecs", bikeSpecs)
                if (emergencyContactName != null) put("emergencyContactName", emergencyContactName)
                if (emergencyContactPhone != null) put("emergencyContactPhone", emergencyContactPhone)
            }

            OutputStreamWriter(profileConn.outputStream, "UTF-8").use {
                it.write(profilePayload.toString())
                it.flush()
            }
            val profileCode = profileConn.responseCode

            // 2. Save to /api/user/bike-management
            val bikeUrl = URL("$serverBaseUrl/api/user/bike-management")
            bikeConn = (bikeUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (!authState.token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer ${authState.token}")
                }
                connectTimeout = 8000
                readTimeout = 12000
                doOutput = true
                doInput = true
            }

            val bikePayload = JSONObject().apply {
                put("isConfigured", true)
                put("specs", JSONObject().apply {
                    put("makeModel", bikeModel)
                    put("engine", bikeSpecs)
                    put("currentOdometer", bikeOdo.toDoubleOrNull() ?: 0.0)
                })
                put("documents", JSONObject().apply {
                    put("rcNumber", bikePlate)
                })
            }

            OutputStreamWriter(bikeConn.outputStream, "UTF-8").use {
                it.write(bikePayload.toString())
                it.flush()
            }
            val bikeCode = bikeConn.responseCode

            // Also persist locally in SharedPreferences
            val bikePrefs = context.applicationContext.getSharedPreferences("rhody_bike_prefs", Context.MODE_PRIVATE)
            bikePrefs.edit()
                .putString("bike_model", bikeModel)
                .putString("bike_plate", bikePlate)
                .putString("bike_specs", bikeSpecs)
                .putString("bike_odo", bikeOdo)
                .putBoolean("is_configured", true)
                .apply()

            if (profileCode in 200..299 || bikeCode in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP Profile: $profileCode, Bike: $bikeCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            profileConn?.disconnect()
            bikeConn?.disconnect()
        }
    }

    suspend fun dispatchCrashSosToMongo(
        latitude: Double,
        longitude: Double,
        speedKmh: Float,
        gForce: Float,
        tiltAngle: Float,
        riderName: String,
        riderEmail: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$serverBaseUrl/api/emergency/crash-event")
            val authState = com.roadpulse.app.data.auth.AuthManager.authState.value
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (!authState.token.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer ${authState.token}")
                }
                connectTimeout = 5000
                readTimeout = 8000
                doOutput = true
                doInput = true
            }

            val payload = JSONObject().apply {
                put("riderName", riderName)
                put("riderEmail", riderEmail)
                put("latitude", latitude)
                put("longitude", longitude)
                put("speedKmh", speedKmh.toDouble())
                put("impactG", gForce.toDouble())
                put("rollAngle", tiltAngle.toDouble())
                put("timestamp", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date()))
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use {
                it.write(payload.toString())
                it.flush()
            }

            val code = connection.responseCode
            if (code in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("HTTP $code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}


