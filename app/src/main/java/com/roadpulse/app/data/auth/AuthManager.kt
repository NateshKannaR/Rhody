package com.roadpulse.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.roadpulse.app.data.sync.CloudSyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val role: String = "rider"
)

data class AuthState(
    val isLoggedIn: Boolean = false,
    val user: AuthUser? = null,
    val token: String? = null
)

class CloudHttpException(val status: Int, message: String) : IOException(message)

object AuthManager {
    private lateinit var prefs: SharedPreferences
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("roadpulse_auth_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null) ?: return
        if (token.length != 43) {
            clearLocalSession()
            return
        }
        val id = prefs.getString("user_id", null) ?: return
        val email = prefs.getString("user_email", null) ?: return
        val name = prefs.getString("user_name", null) ?: email
        val role = prefs.getString("user_role", null) ?: "rider"
        _authState.value = AuthState(true, AuthUser(id, email, name, role), token)
        sessionScope.launch {
            cloudRequest("/api/auth/me", token = token)
                .onSuccess { response ->
                    if (_authState.value.token == token) {
                        val user = parseUser(response.optJSONObject("user") ?: response)
                        saveSession(user, token)
                    }
                }
                .onFailure { error ->
                    if (_authState.value.token == token && (error as? CloudHttpException)?.status == 401) {
                        clearLocalSession()
                    }
                }
        }
    }

    suspend fun login(email: String, password: String): Result<AuthUser> =
        authenticate("/api/auth/login", JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        })

    suspend fun register(
        email: String,
        password: String,
        name: String,
        role: String = "rider",
        riderEmail: String = ""
    ): Result<AuthUser> = authenticate("/api/auth/register", JSONObject().apply {
        put("email", email.trim())
        put("password", password)
        put("name", name.trim())
        put("role", role)
        put("wardCodeOrEmail", riderEmail.trim())
    })

    private suspend fun authenticate(path: String, payload: JSONObject): Result<AuthUser> =
        cloudRequest(path, method = "POST", body = payload, token = null).mapCatching { response ->
            val user = parseUser(response.getJSONObject("user"))
            saveSession(user, response.getString("token"))
            user
        }

    suspend fun cloudRequest(
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        token: String? = _authState.value.token
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("${CloudSyncManager.serverBaseUrl}$path").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                }
            }
            if (body != null) connection.outputStream.writer(Charsets.UTF_8).use { it.write(body.toString()) }
            val status = connection.responseCode
            if (status == 204) return@withContext Result.success(JSONObject())
            val valid = status in 200..299
            val raw = (if (valid) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }
            if (valid) {
                Result.success(JSONObject(raw ?: "{}"))
            } else {
                val error = runCatching { JSONObject(raw ?: "{}").optString("error") }.getOrNull()
                Result.failure(CloudHttpException(status, error?.ifBlank { null } ?: "Rhody Cloud request failed."))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(IOException("Could not connect to Rhody Cloud. Try again.", error))
        } finally {
            connection?.disconnect()
        }
    }

    fun logout() {
        val token = _authState.value.token
        clearLocalSession()
        if (token != null) sessionScope.launch {
            cloudRequest("/api/auth/logout", method = "POST", token = token)
        }
    }

    private fun clearLocalSession() {
        prefs.edit().clear().apply()
        _authState.value = AuthState()
    }

    private fun parseUser(json: JSONObject): AuthUser = AuthUser(
        id = json.getString("id"),
        email = json.getString("email"),
        name = json.optString("name", json.getString("email")),
        role = json.optString("role", "rider")
    )

    private fun saveSession(user: AuthUser, token: String) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_id", user.id)
            .putString("user_email", user.email)
            .putString("user_name", user.name)
            .putString("user_role", user.role)
            .putString("auth_token", token)
            .apply()
        _authState.value = AuthState(true, user, token)
    }
}
