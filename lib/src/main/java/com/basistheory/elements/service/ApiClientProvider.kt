package com.basistheory.elements.service

import com.basistheory.BasisTheoryApiClient
import com.basistheory.elements.model.Environment as ElementsEnvironment
import com.basistheory.elements.util.getApiUrl
import com.basistheory.resources.enrichments.EnrichmentsClient
import com.basistheory.resources.sessions.SessionsClient
import com.basistheory.resources.tokenintents.TokenIntentsClient
import com.basistheory.resources.tokens.TokensClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class ApiClientProvider(
    private val apiUrl: String? = null,
    private val defaultApiKey: String? = null,
    private val environment: ElementsEnvironment = ElementsEnvironment.DEFAULT
) {
    /**
     * One URL for every client this provider creates. An explicitly supplied apiUrl
     * wins, then the named environment, then the compatibility host. Resolving once
     * keeps the proxy and the generated clients on the same origin.
     */
    internal val resolvedApiUrl: String = apiUrl ?: environment.getApiUrl()

    fun getTokensApi(apiKeyOverride: String? = null): TokensClient =
        getApiClient(apiKeyOverride).tokens()

    fun getSessionsApi(apiKeyOverride: String? = null): SessionsClient =
        getApiClient(apiKeyOverride).sessions()

    fun getTokenIntentsApi(apiKeyOverride: String? = null): TokenIntentsClient =
        getApiClient(apiKeyOverride).tokenIntents()

    fun getEnrichmentsApi(apiKeyOverride: String? = null): EnrichmentsClient =
        getApiClient(apiKeyOverride).enrichments()

    fun getProxyApi(dispatcher: CoroutineDispatcher = Dispatchers.IO): ProxyApi {
        requireNotNull(defaultApiKey)

       return ProxyApi(dispatcher, resolvedApiUrl, defaultApiKey)
    }

    private fun getApiClient(apiKeyOverride: String? = null): BasisTheoryApiClient {
        val apiKey = apiKeyOverride ?: defaultApiKey
        requireNotNull(apiKey)

        return BasisTheoryApiClient.builder()
            .apiKey(apiKey)
            .url(resolvedApiUrl)
            .httpClient(createHttpClientWithDeviceInfo())
            .build()
    }
}