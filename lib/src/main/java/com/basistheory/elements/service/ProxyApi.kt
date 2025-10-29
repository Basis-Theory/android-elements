package com.basistheory.elements.service

import com.basistheory.elements.model.ElementValueReference
import com.basistheory.elements.model.exceptions.ApiException
import com.basistheory.elements.util.isPrimitiveType
import com.basistheory.elements.util.replaceElementRefs
import com.basistheory.elements.util.toMap
import com.basistheory.elements.util.transformResponseToValueReferences
import com.basistheory.elements.view.TextElement
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


interface Proxy {

    suspend fun get(proxyRequest: ProxyRequest, apiKeyOverride: String? = null): Any?

    suspend fun post(proxyRequest: ProxyRequest, apiKeyOverride: String? = null): Any?

    suspend fun put(proxyRequest: ProxyRequest, apiKeyOverride: String? = null): Any?

    suspend fun patch(proxyRequest: ProxyRequest, apiKeyOverride: String? = null): Any?

    suspend fun delete(proxyRequest: ProxyRequest, apiKeyOverride: String? = null): Any?
}

enum class HttpMethod {
    GET,
    POST,
    PATCH,
    PUT,
    DELETE
}

const val BT_EXPOSE_RAW_PROXY_RESPONSE_HEADER = "bt-expose-raw-proxy-response"

class ProxyRequest {
    var path: String? = null
    var queryParams: Map<String, String>? = emptyMap()
    var headers: Map<String, String>? = emptyMap()
    var body: Any? = null
}

class ProxyApi(
    val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    val apiBaseUrl: String = "https://api.basistheory.com",
    val apiKey: String,
    val httpClient: OkHttpClient = OkHttpClient()
) : Proxy {

    override suspend fun get(proxyRequest: ProxyRequest, apiKeyOverride: String?): Any? =
        withContext(dispatcher) {
            proxy(HttpMethod.GET.name, proxyRequest, apiKeyOverride)
        }

    override suspend fun post(proxyRequest: ProxyRequest, apiKeyOverride: String?): Any? =
        withContext(dispatcher) {
            proxy(HttpMethod.POST.name, proxyRequest, apiKeyOverride)
        }

    override suspend fun put(proxyRequest: ProxyRequest, apiKeyOverride: String?): Any? =
        withContext(dispatcher) {
            proxy(HttpMethod.PUT.name, proxyRequest, apiKeyOverride)
        }

    override suspend fun patch(proxyRequest: ProxyRequest, apiKeyOverride: String?): Any? =
        withContext(dispatcher) {
            proxy(HttpMethod.PATCH.name, proxyRequest, apiKeyOverride)
        }

    override suspend fun delete(proxyRequest: ProxyRequest, apiKeyOverride: String?): Any? =
        withContext(dispatcher) {
            proxy(HttpMethod.DELETE.name, proxyRequest, apiKeyOverride)
        }

    private fun proxy(method: String, proxyRequest: ProxyRequest, apiKeyOverride: String?): Any? {
        val gson = Gson()

        val processedBody = proxyRequest.body?.let { body ->
            when {
                body::class.java.isPrimitiveType() -> body
                body is TextElement -> body.getTransformedText()
                body is ElementValueReference -> body.getValue()
                else -> replaceElementRefs(body.toMap())
            }
        }

        val urlBuilder = (apiBaseUrl + "/proxy" + (proxyRequest.path.orEmpty()))
            .toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalArgumentException("Invalid URL")
        proxyRequest.queryParams?.toPairs()?.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }
        val url = urlBuilder.build()

        val isTextPlain = proxyRequest.headers
            ?.any {
                it.key.equals("Content-Type", ignoreCase = true) &&
                        it.value.contains("text/plain", ignoreCase = true)
            }
            ?: false

        val requestBody = when {
            method.equals("GET", ignoreCase = true) ||
                    method.equals("DELETE", ignoreCase = true) -> null
            isTextPlain -> processedBody?.toString().orEmpty()
                .toRequestBody("text/plain; charset=utf-8".toMediaTypeOrNull())

            else -> processedBody?.let { gson.toJson(it) }.orEmpty()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        }

        val request = Request.Builder()
            .url(url)
            .method(method.uppercase(), requestBody)
            .apply {
                proxyRequest.headers?.forEach { (key, value) ->
                    addHeader(key, value)
                }
                val apiKey = apiKeyOverride ?: apiKey
                addHeader("BT-API-KEY", apiKey)
            }
            .build()

        httpClient.newCall(request).execute().use { response ->
            val responseStr = response.body?.string().orEmpty()
            val returnType = object : TypeToken<Any?>() {}.type
            val data: Any? = gson.fromJson(responseStr, returnType)
            return if (response.header(BT_EXPOSE_RAW_PROXY_RESPONSE_HEADER) != null)
                data
            else transformResponseToValueReferences(data)
        }
    }

    private fun Map<String, String>.toPairs(): List<Pair<String, String?>> =
        this.map {
            Pair(it.key, it.value)
        }
}

fun Any?.tryGetElementValueReference(path: String): ElementValueReference? {
    if (this == null || path.isEmpty()) return null

    val pathSegments = path.split(".")
    val matchResult = Regex("^(\\w*)(?:\\[(\\d+)])?$").matchEntire(pathSegments.first())
    val pathSegment = matchResult?.groups?.elementAtOrNull(1)?.value
    val indexSegment = matchResult?.groups?.elementAtOrNull(2)?.value?.toIntOrNull()

    val value = pathSegment?.let {
        val map = this as? Map<*, *> ?: return null
        map[pathSegment]?.let {
            if (indexSegment != null) {
                val collection = it as? Collection<*> ?: return null
                collection.elementAt(indexSegment)
            } else it
        }
    }

    return if (pathSegments.count() > 1)
        value?.tryGetElementValueReference(pathSegments.drop(1).joinToString("."))
    else
        value as ElementValueReference?
}

fun Any?.getElementValueReference(path: String): ElementValueReference =
    this.tryGetElementValueReference(path) ?: throw NoSuchElementException()