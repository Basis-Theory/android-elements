package com.basistheory.elements.service

import com.basistheory.core.Environment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class BasisTheoryElementsBuilder {
    private var _apiKey: String? = null
    private var _apiUrl: String = "https://api.basistheory.com"
    private var _environment: Environment = Environment.DEFAULT
    private var _dispatcher: CoroutineDispatcher = Dispatchers.IO

    fun apiKey(value: String): BasisTheoryElementsBuilder {
        _apiKey = value
        return this
    }

    fun apiUrl(value: String): BasisTheoryElementsBuilder {
        _apiUrl = value
        return this
    }

    fun environment(value: Environment): BasisTheoryElementsBuilder {
        _environment = value
        return this
    }

    fun dispatcher(value: CoroutineDispatcher): BasisTheoryElementsBuilder {
        _dispatcher = value
        return this
    }

    fun build(): BasisTheoryElements =
        BasisTheoryElements(
            ApiClientProvider(_apiUrl, _apiKey, _environment),
            _dispatcher
        )
}