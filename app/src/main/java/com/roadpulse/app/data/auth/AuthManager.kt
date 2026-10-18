package com.roadpulse.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.roadpulse.app.data.sync.CloudSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class AuthUser(
    val id: String,
    val email: String,
    val name: String
)

data class AuthState(
    val isLoggedIn: Boolean = false,
    val user: AuthUser? = null,
    val token: String? = null
)

object AuthManager {
    private const val PREFS_NAME = "roadpulse_auth_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_NAME = "user_name"
    private const val KEY_TOKEN = "auth_token"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isLoggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
            if (isLoggedIn) {
                val userId = prefs?.getString(KEY_USER_ID, "") ?: ""
                val email = prefs?.getString(KEY_EMAIL, "") ?: ""
                val name = prefs?.getString(KEY_NAME, "") ?: ""
                val token = prefs?.getString(KEY_TOKEN, "") ?: ""

                if (email.isNotEmpty() && token.isNotEmpty()) {
                    _authState.value = AuthState(
                        isLoggedIn = true,
                        user = AuthUser(id = userId, email = email, name = name),
                        token = token
                    )
                    // Asynchronously sync latest bike and profile from MongoDB Atlas
                    appContext?.let { ctx ->
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            CloudSyncManager.fetchUserProfileAndBike(ctx)
                        }
                    }
                }
            }
        }
    }

    suspend fun login(email: String, password: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("${CloudSyncManager.serverBaseUrl}/api/auth/login")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 12000
                doOutput = true
                doInput = true
            }

            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", password)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val token = json.getString("token")
                val userJson = json.getJSONObject("user")
                val user = AuthUser(
                    id = userJson.getString("id"),
                    email = userJson.getString("email"),
                    name = userJson.optString("name", userJson.getString("email"))
                )

                saveSession(user, token)
                appContext?.let { ctx ->
                    try {
                        CloudSyncManager.fetchUserProfileAndBike(ctx)
                    } catch (_: Exception) {}
                }
                Result.success(user)
            } else {
                val errText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                val errMsg = try {
                    JSONObject(errText).optString("error", "Login failed ($responseCode)")
                } catch (_: Exception) {
                    "Login failed ($responseCode)"
                }
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error"))
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun register(email: String, password: String, name: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("${CloudSyncManager.serverBaseUrl}/api/auth/register")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                connectTimeout = 8000
                readTimeout = 12000
                doOutput = true
                doInput = true
            }

            val payload = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", password)
                put("name", name.trim())
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val token = json.getString("token")
                val userJson = json.getJSONObject("user")
                val user = AuthUser(
                    id = userJson.getString("id"),
                    email = userJson.getString("email"),
                    name = userJson.optString("name", userJson.getString("email"))
                )

                saveSession(user, token)
                appContext?.let { ctx ->
                    try {
                        CloudSyncManager.fetchUserProfileAndBike(ctx)
                    } catch (_: Exception) {}
                }
                Result.success(user)
            } else {
                val errText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                val errMsg = try {
                    JSONObject(errText).optString("error", "Registration failed ($responseCode)")
                } catch (_: Exception) {
                    "Registration failed ($responseCode)"
                }
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error"))
        } finally {
            connection?.disconnect()
        }
    }

    fun logout() {
        prefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_EMAIL)
            remove(KEY_NAME)
            remove(KEY_TOKEN)
            apply()
        }
        _authState.value = AuthState()
    }

    private fun saveSession(user: AuthUser, token: String) {
        prefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.id)
            putString(KEY_EMAIL, user.email)
            putString(KEY_NAME, user.name)
            putString(KEY_TOKEN, token)
            apply()
        }
        _authState.value = AuthState(
            isLoggedIn = true,
            user = user,
            token = token
        )
    }
}
