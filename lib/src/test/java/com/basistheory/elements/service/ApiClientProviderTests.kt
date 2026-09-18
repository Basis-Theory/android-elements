package com.basistheory.elements.service

import com.basistheory.elements.model.Environment
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ApiClientProviderTests {

    @Test
    fun `uses the compatibility host when nothing is configured`() {
        expectThat(ApiClientProvider(defaultApiKey = "key").resolvedApiUrl)
            .isEqualTo("https://api.basistheory.com")
    }

    @Test
    fun `uses the compatibility host for the default environment`() {
        expectThat(
            ApiClientProvider(defaultApiKey = "key", environment = Environment.DEFAULT).resolvedApiUrl
        ).isEqualTo("https://api.basistheory.com")
    }

    @Test
    fun `resolves the us environment to its regional host`() {
        expectThat(
            ApiClientProvider(defaultApiKey = "key", environment = Environment.US).resolvedApiUrl
        ).isEqualTo("https://api.us.basistheory.com")
    }

    @Test
    fun `resolves the eu environment to its regional host`() {
        expectThat(
            ApiClientProvider(defaultApiKey = "key", environment = Environment.EU).resolvedApiUrl
        ).isEqualTo("https://api.eu.basistheory.com")
    }

    @Test
    fun `resolves the test environment`() {
        expectThat(
            ApiClientProvider(defaultApiKey = "key", environment = Environment.TEST).resolvedApiUrl
        ).isEqualTo("https://api.test.basistheory.com")
    }

    @Test
    fun `an explicit apiUrl takes precedence over the environment`() {
        expectThat(
            ApiClientProvider(
                apiUrl = "https://my-custom-api.basistheory.com",
                defaultApiKey = "key",
                environment = Environment.EU
            ).resolvedApiUrl
        ).isEqualTo("https://my-custom-api.basistheory.com")
    }

    @Test
    fun `proxy requests use the same resolved url as the other clients`() {
        val provider = ApiClientProvider(defaultApiKey = "key", environment = Environment.EU)

        expectThat(provider.getProxyApi().apiBaseUrl)
            .isEqualTo(provider.resolvedApiUrl)
            .isEqualTo("https://api.eu.basistheory.com")
    }

    @Test
    fun `proxy requests honour an explicit apiUrl`() {
        val provider = ApiClientProvider(
            apiUrl = "https://my-custom-api.basistheory.com",
            defaultApiKey = "key",
            environment = Environment.EU
        )

        expectThat(provider.getProxyApi().apiBaseUrl)
            .isEqualTo("https://my-custom-api.basistheory.com")
    }
}
