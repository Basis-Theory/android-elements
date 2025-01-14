package com.basistheory.elements.service

import com.basistheory.ApiClient
import com.basistheory.Configuration
import com.basistheory.SessionsApi
import com.basistheory.TokenizeApi
import com.basistheory.TokensApi
import com.basistheory.elements.BuildConfig
import com.basistheory.auth.ApiKeyAuth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class ApiClientProvider(
    private val apiUrl: String = "https://api.basistheory.com",
    private val defaultApiKey: String? = null
) {

    fun getTokenizeApi(apiKeyOverride: String? = null): TokenizeApi =
        TokenizeApi(getApiClient(apiKeyOverride))

    fun getTokensApi(apiKeyOverride: String? = null): TokensApi =
        TokensApi(getApiClient(apiKeyOverride))

    fun getSessionsApi(apiKeyOverride: String? = null): SessionsApi =
        SessionsApi(getApiClient(apiKeyOverride))

    fun getProxyApi(dispatcher: CoroutineDispatcher = Dispatchers.IO): ProxyApi =
        ProxyApi(dispatcher, ::getApiClient)

    private fun getApiClient(apiKeyOverride: String? = null): ApiClient {
        val apiKey = apiKeyOverride ?: defaultApiKey
        requireNotNull(apiKey)

        return Configuration.getDefaultApiClient().also { client ->
            client.basePath = apiUrl

            val userAgent = "android-elements/${BuildConfig.VERSION_NAME} ${System.getProperty("http.agent") ?: ""}".trim()
            client.setUserAgent(userAgent)

            (client.getAuthentication("ApiKey") as ApiKeyAuth).also { auth ->
                auth.apiKey = apiKey
            }
        }
    }
}
