package com.basistheory.elements.util

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.Base64
import com.google.gson.Gson
import java.util.*

data class DeviceInfo(
    val uaBrands: List<Map<String, String>>? = null,
    val uaMobile: Boolean? = null,
    val uaPlatform: String? = null,
    val uaPlatformVersion: String? = null,
    val languages: List<String>? = null,
    val timeZone: String? = null,
    val platform: String? = null,
    val screenWidthPixels: Double? = null,
    val screenHeightPixels: Double? = null,
    val innerWidthPoints: Double? = null,
    val innerHeightPoints: Double? = null,
    val devicePixelRatio: Double? = null
)

private fun getApplicationContext(): Context? {
    return try {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
        currentApplicationMethod.invoke(null) as? Application
    } catch (e: Exception) {
        null
    }
}

fun DeviceInfo.encoded(): String? {
    return try {
        val gson = Gson()
        val json = gson.toJson(this)
        Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }
}

fun getEncodedDeviceInfo(): String? {
    return try {
        val context = getApplicationContext() ?: return null
        val displayMetrics = Resources.getSystem().displayMetrics
        val density = displayMetrics.density
        val platformVersion = Build.VERSION.RELEASE
        
        val preferredLanguages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.let { locales ->
                (0 until locales.size()).map { locales[it].toString() }
            }
        } else {
            @Suppress("DEPRECATION")
            listOf(context.resources.configuration.locale.toString())
        }

        val timeZone = TimeZone.getDefault().id

        val platform = "Android"
        
        val isMobile = (displayMetrics.widthPixels / density) < 600
        
        val deviceInfo = DeviceInfo(
            uaBrands = null,
            uaMobile = isMobile,
            uaPlatform = platform,
            uaPlatformVersion = platformVersion,
            languages = preferredLanguages,
            timeZone = timeZone,
            platform = platform,
            screenWidthPixels = displayMetrics.widthPixels.toDouble(),
            screenHeightPixels = displayMetrics.heightPixels.toDouble(),
            innerWidthPoints = (displayMetrics.widthPixels / density).toDouble(),
            innerHeightPoints = (displayMetrics.heightPixels / density).toDouble(),
            devicePixelRatio = density.toDouble()
        )
        
        deviceInfo.encoded()
    } catch (e: Exception) {
        null
    }
}
