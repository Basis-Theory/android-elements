package com.basistheory.elements.service

import com.basistheory.BasisTheoryApiClient
import com.basistheory.resources.sessions.SessionsClient
import com.basistheory.resources.tokenintents.TokenIntentsClient
import com.basistheory.resources.tokens.TokensClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class ApiClientProvider(
    private val apiUrl: String = "https://api.basistheory.com",
    private val defaultApiKey: String? = null
) {
    fun getTokensApi(apiKeyOverride: String? = null): TokensClient =
        getApiClient(apiKeyOverride).tokens()

    fun getSessionsApi(apiKeyOverride: String? = null): SessionsClient =
        getApiClient(apiKeyOverride).sessions()

    fun getTokenIntentsApi(apiKeyOverride: String? = null): TokenIntentsClient =
        getApiClient(apiKeyOverride).tokenIntents()

    fun getProxyApi(dispatcher: CoroutineDispatcher = Dispatchers.IO): ProxyApi {
        requireNotNull(defaultApiKey)

       return ProxyApi(dispatcher, apiUrl, defaultApiKey)
    }

    private fun getApiClient(apiKeyOverride: String? = null): BasisTheoryApiClient {
        val apiKey = apiKeyOverride ?: defaultApiKey
        requireNotNull(apiKey)

        return BasisTheoryApiClient.builder().apiKey(apiKey).url(apiUrl).build()
    }
}