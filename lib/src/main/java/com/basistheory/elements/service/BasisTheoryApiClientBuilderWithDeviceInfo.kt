package com.basistheory.elements.service

import com.basistheory.elements.util.getEncodedDeviceInfo
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

class DeviceInfoInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        
        getEncodedDeviceInfo()?.let { deviceInfo ->
            requestBuilder.addHeader("BT-DEVICE-INFO", deviceInfo)
        }
        
        return chain.proceed(requestBuilder.build())
    }
}

fun createHttpClientWithDeviceInfo(): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(DeviceInfoInterceptor())
        .build()
}
