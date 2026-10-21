package com.remoteforge.app.data.config

import android.content.Context
import android.content.SharedPreferences
import com.remoteforge.app.BuildConfig
import com.remoteforge.app.data.api.RemoteForgeApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NetworkConfig(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("remoteforge_prefs", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("server_url", BuildConfig.DEFAULT_SERVER_URL) ?: BuildConfig.DEFAULT_SERVER_URL
        set(value) {
            val formatted = value.trim().removeSuffix("/")
            prefs.edit().putString("server_url", formatted).apply()
            rebuildRetrofit(formatted)
        }

    var authToken: String?
        get() = prefs.getString("auth_token", null)
        set(value) = prefs.edit().putString("auth_token", value).apply()

    var savedUsername: String?
        get() = prefs.getString("saved_username", "admin")
        set(value) = prefs.edit().putString("saved_username", value).apply()

    var savedPassword: String?
        get() = prefs.getString("saved_password", "remoteforge_secure_pass")
        set(value) = prefs.edit().putString("saved_password", value).apply()

    var primaryDeviceId: String?
        get() = prefs.getString("primary_device_id", null)
        set(value) = prefs.edit().putString("primary_device_id", value).apply()

    companion object {
        const val MODE_APP_SESSION = "APP_SESSION"
        const val MODE_STAY_CONNECTED = "STAY_CONNECTED"
        const val DEFAULT_OVERLAY_URL = "http://100.117.133.109:8090"
    }

    var connectionMode: String
        get() = prefs.getString("connection_mode", MODE_APP_SESSION) ?: MODE_APP_SESSION
        set(value) = prefs.edit().putString("connection_mode", value).apply()

    var stableVpnUrl: String
        get() = prefs.getString("stable_vpn_url", DEFAULT_OVERLAY_URL) ?: DEFAULT_OVERLAY_URL
        set(value) = prefs.edit().putString("stable_vpn_url", value.trim().removeSuffix("/")).apply()

    var autoVpnEnabled: Boolean
        get() = prefs.getBoolean("auto_vpn_enabled", true)
        set(value) = prefs.edit().putBoolean("auto_vpn_enabled", value).apply()

    private val _appearanceThemeFlow = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("appearance_theme", "DARK") ?: "DARK")
    val appearanceThemeFlow: kotlinx.coroutines.flow.StateFlow<String> = _appearanceThemeFlow

    var appearanceTheme: String
        get() = prefs.getString("appearance_theme", "DARK") ?: "DARK"
        set(value) {
            prefs.edit().putString("appearance_theme", value).apply()
            _appearanceThemeFlow.value = value
        }

    private val _animationIntensityFlow = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("animation_intensity", "FULL") ?: "FULL")
    val animationIntensityFlow: kotlinx.coroutines.flow.StateFlow<String> = _animationIntensityFlow

    var animationIntensity: String
        get() = prefs.getString("animation_intensity", "FULL") ?: "FULL"
        set(value) {
            prefs.edit().putString("animation_intensity", value).apply()
            _animationIntensityFlow.value = value
        }

    private val _reducedMotionFlow = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("reduced_motion", false))
    val reducedMotionFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _reducedMotionFlow

    var reducedMotion: Boolean
        get() = prefs.getBoolean("reduced_motion", false)
        set(value) {
            prefs.edit().putBoolean("reduced_motion", value).apply()
            _reducedMotionFlow.value = value
        }

    var soundEffects: Boolean
        get() = prefs.getBoolean("sound_effects", false)
        set(value) = prefs.edit().putBoolean("sound_effects", value).apply()

    var bootSequenceSeen: Boolean
        get() = prefs.getBoolean("boot_sequence_seen", false)
        set(value) = prefs.edit().putBoolean("boot_sequence_seen", value).apply()

    fun clearSession() {
        prefs.edit()
            .remove("auth_token")
            .remove("saved_password")
            .apply()
    }

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        OkHttpClient.Builder()
            .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    private var retrofitInstance: Retrofit? = null
    var apiService: RemoteForgeApiService? = null
        private set

    init {
        rebuildRetrofit(serverUrl)
    }

    fun rebuildRetrofit(baseUrl: String) {
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        retrofitInstance = Retrofit.Builder()
            .baseUrl(safeUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofitInstance?.create(RemoteForgeApiService::class.java)
    }
}
