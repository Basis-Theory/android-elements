package com.basistheory.elements.service

import HttpClient
import com.basistheory.elements.constants.ElementValueType
import com.basistheory.elements.model.CreateSessionResponse
import com.basistheory.elements.model.CreateTokenRequest
import com.basistheory.elements.model.ElementValueReference
import com.basistheory.elements.model.Token
import com.basistheory.elements.model.exceptions.ApiException
import com.basistheory.elements.model.toAndroid
import com.basistheory.elements.model.toJava
import com.basistheory.elements.util.*
import com.basistheory.elements.util.getElementsValues
import com.basistheory.elements.util.replaceElementRefs
import com.basistheory.elements.util.tryGetTextToTokenize
import com.basistheory.elements.view.TextElement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BasisTheoryElements internal constructor(
    private val apiClientProvider: ApiClientProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val proxy: ProxyApi = apiClientProvider.getProxyApi(dispatcher)
    val client = HttpClient(dispatcher)

    @JvmOverloads
    suspend fun tokenize(body: Any, apiKeyOverride: String? = null): Any =
        try {
            withContext(dispatcher) {
                val tokenizeApiClient = apiClientProvider.getTokenizeApi(apiKeyOverride)
                val request = getElementsValues(body)

                tokenizeApiClient.tokenize(request)
            }
        } catch (e: com.basistheory.ApiException) {
            throw ApiException(e.code, e.responseHeaders, e.responseBody, e.message)
        }

    @JvmOverloads
    suspend fun createToken(
        createTokenRequest: CreateTokenRequest,
        apiKeyOverride: String? = null
    ): Token =
        try {
            withContext(dispatcher) {
                val tokensApi = apiClientProvider.getTokensApi(apiKeyOverride)
                val data =
                    if (createTokenRequest.data::class.java.isPrimitiveType()) createTokenRequest.data
                    else if (createTokenRequest.data is TextElement) (createTokenRequest.data as TextElement).tryGetTextToTokenize()
                        .toValueType((createTokenRequest.data as TextElement).getValueType)
                    else if (createTokenRequest.data is ElementValueReference) (createTokenRequest.data as ElementValueReference).getValue()
                        .toValueType((createTokenRequest.data as ElementValueReference).getValueType)
                    else replaceElementRefs(createTokenRequest.data.toMap())

                createTokenRequest.data = data!!

                tokensApi.create(createTokenRequest.toJava()).toAndroid()
            }
        } catch (e: com.basistheory.ApiException) {
            throw ApiException(e.code, e.responseHeaders, e.responseBody, e.message)
        }

    @JvmOverloads
    suspend fun createSession(apiKeyOverride: String? = null): CreateSessionResponse =
        try {
            withContext(dispatcher) {
                val sessionsApi = apiClientProvider.getSessionsApi(apiKeyOverride)
                sessionsApi.create().toAndroid()
            }
        } catch (e: com.basistheory.ApiException) {
            throw ApiException(e.code, e.responseHeaders, e.responseBody, e.message)
        }

    @JvmOverloads
    suspend fun getToken(
        id: String,
        apiKeyOverride: String? = null
    ): Token =
        try {
            withContext(dispatcher) {
                val tokensApi = apiClientProvider.getTokensApi(apiKeyOverride)

                tokensApi.getById(id).also {
                    it.data = transformResponseToValueReferences(it.data)
                }.toAndroid()
            }
        } catch (e: com.basistheory.ApiException) {
            throw ApiException(e.code, e.responseHeaders, e.responseBody, e.message)
        }

    companion object {
        @JvmStatic
        fun builder(): BasisTheoryElementsBuilder = BasisTheoryElementsBuilder()
    }
}

fun String?.toValueType(getValueType: ElementValueType?): Any? {
    return when (getValueType) {
        ElementValueType.INTEGER -> {
            this?.toInt()
        }

        ElementValueType.DOUBLE -> {
            this?.toDouble()
        }

        ElementValueType.BOOLEAN -> {
            this?.toBoolean()
        }

        else -> {
            this;
        }
    }
}

fun <T> Any?.getValue(path: String): T = this.tryGetValue(path) ?: throw NoSuchElementException()

fun <T> Any?.tryGetValue(path: String): T? {
    if (this == null || path.isEmpty()) return null

    val pathSegments = path.split(".")
    val map = this as? Map<*, *> ?: return null

    val value = map[pathSegments.first()]

    return if (pathSegments.count() > 1)
        value?.tryGetValue(pathSegments.drop(1).joinToString("."))
    else
        value as? T?
}
