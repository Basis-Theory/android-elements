package com.basistheory.elements.service

import kotlinx.coroutines.Dispatchers
import org.junit.Test
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isSuccess

class BasisTheoryElementsBuilderTests {

    @Test
    fun `can't build default instance without an api key`() {
        expectCatching {
            BasisTheoryElements.builder()
                .build()
        }.isFailure()
    }

    @Test
    fun `can build instance with custom parameters`() {
        expectCatching {
            BasisTheoryElements.builder()
                .apiKey("api_key")
                .apiUrl("https://my-custom-api.basistheory.com")
                .dispatcher(Dispatchers.Unconfined)
                .build()
        }.isSuccess().isA<BasisTheoryElements>()
    }
}
