package com.basistheory.elements.util

import android.content.res.Resources
import android.os.Build
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.*
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class DeviceInfoTests {

    @Test
    fun `getEncodedDeviceInfo returns encoded device info`() {
        val result = getEncodedDeviceInfo()

        expectThat(result).isNotNull()

        // Decode and verify structure
        val decoded = String(java.util.Base64.getDecoder().decode(result))
        val gson = Gson()
        val deviceInfo = gson.fromJson(decoded, DeviceInfo::class.java)

        expectThat(deviceInfo) {
            get { uaBrands }.isNull()
            get { uaMobile }.isNotNull()
            get { uaPlatform }.isEqualTo("Android")
            get { uaPlatformVersion }.isNotNull()
            get { languages }.isNotNull().isNotEmpty()
            get { timeZone }.isNotNull()
            get { platform }.isEqualTo("Android")
            get { screenWidthPixels }.isNotNull().isGreaterThan(0.0)
            get { screenHeightPixels }.isNotNull().isGreaterThan(0.0)
            get { innerWidthPoints }.isNotNull().isGreaterThan(0.0)
            get { innerHeightPoints }.isNotNull().isGreaterThan(0.0)
            get { devicePixelRatio }.isNotNull().isGreaterThan(0.0)
        }
    }

    @Test
    fun `getEncodedDeviceInfo includes platform version`() {
        val result = getEncodedDeviceInfo()

        expectThat(result).isNotNull()

        val decoded = String(java.util.Base64.getDecoder().decode(result))
        val gson = Gson()
        val deviceInfo = gson.fromJson(decoded, DeviceInfo::class.java)

        expectThat(deviceInfo.uaPlatformVersion).isEqualTo(Build.VERSION.RELEASE)
    }

    @Test
    fun `getEncodedDeviceInfo includes system timezone`() {
        val result = getEncodedDeviceInfo()

        expectThat(result).isNotNull()

        val decoded = String(java.util.Base64.getDecoder().decode(result))
        val gson = Gson()
        val deviceInfo = gson.fromJson(decoded, DeviceInfo::class.java)

        expectThat(deviceInfo.timeZone).isEqualTo(TimeZone.getDefault().id)
    }

    @Test
    fun `getEncodedDeviceInfo calculates isMobile based on screen width`() {
        val result = getEncodedDeviceInfo()

        expectThat(result).isNotNull()

        val decoded = String(java.util.Base64.getDecoder().decode(result))
        val gson = Gson()
        val deviceInfo = gson.fromJson(decoded, DeviceInfo::class.java)

        val displayMetrics = Resources.getSystem().displayMetrics
        val density = displayMetrics.density
        val expectedIsMobile = (displayMetrics.widthPixels / density) < 600
    
        expectThat(deviceInfo.uaMobile).isEqualTo(expectedIsMobile)
    }

    @Test
    fun `getEncodedDeviceInfo includes screen dimensions`() {
        val result = getEncodedDeviceInfo()

        expectThat(result).isNotNull()

        val decoded = String(java.util.Base64.getDecoder().decode(result))
        val gson = Gson()
        val deviceInfo = gson.fromJson(decoded, DeviceInfo::class.java)

        val displayMetrics = Resources.getSystem().displayMetrics

        expectThat(deviceInfo) {
            get { screenWidthPixels }.isEqualTo(displayMetrics.widthPixels.toDouble())
            get { screenHeightPixels }.isEqualTo(displayMetrics.heightPixels.toDouble())
            get { devicePixelRatio }.isEqualTo(displayMetrics.density.toDouble())
        }
    }
}
