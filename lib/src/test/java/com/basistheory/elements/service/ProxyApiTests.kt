package com.basistheory.elements.service

import com.basistheory.elements.model.ElementValueReference
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.*
import java.util.*


@RunWith(JUnitParamsRunner::class)
class ProxyApiTests {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var mockHttpClient: OkHttpClient

    @MockK
    private lateinit var mockCall: Call

    @MockK
    private lateinit var mockResponse: Response

    @MockK
    private lateinit var mockResponseBody: ResponseBody

    private lateinit var proxyApi: ProxyApi

    private var proxyRequest: ProxyRequest = ProxyRequest()

    private fun setupMocks(responseJson: String = "{}", exposeRawResponse: Boolean = false): CapturingSlot<Request> {
        val requestSlot = slot<Request>()
        
        every { mockHttpClient.newCall(capture(requestSlot)) } returns mockCall
        every { mockCall.execute() } returns mockResponse
        every { mockResponse.body } returns mockResponseBody
        every { mockResponseBody.string() } returns responseJson
        
        if (exposeRawResponse) {
            every { mockResponse.header(BT_EXPOSE_RAW_PROXY_RESPONSE_HEADER) } returns "true"
        } else {
            every { mockResponse.header(any()) } returns null
        }
        
        every { mockResponse.close() } just runs
        
        return requestSlot
    }

    @Before
    fun setup() {
        proxyApi = ProxyApi(
            dispatcher = Dispatchers.IO,
            apiBaseUrl = "https://api.basistheory.com",
            apiKey = "124",
            httpClient = mockHttpClient
        )
        proxyRequest = ProxyRequest()

        setupMocks()
    }

    @Test
    @Parameters(method = "proxyMethodsTestsInput")
    fun `should execute proxy request with the provided values`(
        httpMethod: HttpMethod,
        contentType: String?,
        contentsSubType: String?,
        requestBody: String?
    ) {
        val queryParamValue = UUID.randomUUID().toString()
        proxyRequest = proxyRequest.apply {
            path = "/payment"
            headers = mapOf(
                "BT-PROXY-URL" to "https://echo.basistheory.com/post",
                "Content-Type" to "text/plain"
            )
            queryParams = mapOf("param" to queryParamValue)
            body = requestBody
        }

        val requestSlot = setupMocks("\"Hello World\"")

        val result = runBlocking {
            when (httpMethod) {
                HttpMethod.GET -> proxyApi.get(proxyRequest)
                HttpMethod.POST -> proxyApi.post(proxyRequest)
                HttpMethod.PATCH -> proxyApi.patch(proxyRequest)
                HttpMethod.PUT -> proxyApi.put(proxyRequest)
                HttpMethod.DELETE -> proxyApi.delete(proxyRequest)
            }
        }

        verify(exactly = 1) { mockHttpClient.newCall(any()) }
        verify(exactly = 1) { mockCall.execute() }

        expectThat(requestSlot.captured) {
            get { method }.isEqualTo(httpMethod.name)
            get { url.toString() }
                .isEqualTo("https://api.basistheory.com/proxy/payment?param=${queryParamValue}")
            get { headers["BT-PROXY-URL"] }.isEqualTo("https://echo.basistheory.com/post")
            get { body?.contentType()?.type }.isEqualTo(contentType)
            get { body?.contentType()?.subtype }.isEqualTo(contentsSubType)

            if (this.subject.body != null) {
                val buffer = Buffer()
                this.subject.body!!.writeTo(buffer)
                val bodyInRequest = buffer.readUtf8()
                expectThat(bodyInRequest).isEqualTo(requestBody)
            } else {
                get { body }.isNull()
            }
        }

        expectThat(result).isA<ElementValueReference>()
    }

    @Test
    fun `should transform complex proxy response to element value references`() {
        val responseJson = """
            {
                "customer_id": "102023201931949",
                "id": null,
                "card": {
                    "number": "4242424242424242",
                    "expiration_month": "10",
                    "expiration_year": "2026",
                    "cvc": "123"
                },
                "pii": {
                    "name": {
                        "first_name": "Drewsue",
                        "last_name": "Webuino"
                    },
                    "phone_numbers": ["+1 111 222 3333", "+1 999 888 7777"],
                    "aliases": [
                        {
                            "first_name": "John",
                            "last_name": "Doe"
                        }
                    ]
                }
            }
        """.trimIndent()

        setupMocks(responseJson)

        val result = runBlocking {
            proxyApi.post(proxyRequest)
        }

        expectThat(result.tryGetElementValueReference("id")).isNull()
        expectThat(result.tryGetElementValueReference("invalid_path")).isNull()
        expectThat((result.tryGetElementValueReference("customer_id"))).isNotNull()

        expectThat(result.tryGetElementValueReference("card.number")).isNotNull()
        expectThat(result.tryGetElementValueReference("card.expiration_month")).isNotNull()
        expectThat(result.tryGetElementValueReference("card.expiration_year")).isNotNull()
        expectThat(result.tryGetElementValueReference("card.cvc")).isNotNull()

        expectThat(result.tryGetElementValueReference("pii.name.first_name")).isNotNull()
        expectThat(result.tryGetElementValueReference("pii.name.last_name")).isNotNull()
        expectThat(result.tryGetElementValueReference("pii.phone_numbers[0]")).isNotNull()
        expectThat(result.tryGetElementValueReference("pii.aliases[0].first_name")).isNotNull()
    }

    @Test
    fun `should return raw response when BT_EXPOSE_RAW_PROXY_RESPONSE_HEADER is present`() {
        val responseJson = """{"test":"something something"}"""

        setupMocks(responseJson, true)

        val result = runBlocking {
            proxyApi.post(proxyRequest)
        }

        expectThat(result.toString()).isEqualTo("{test=something something}")
    }

    @Test
    fun `should transform array proxy response to element value references`() {
        val responseJson = """["foo", null, "bar", null, "yaz", "qux"]"""

        setupMocks(responseJson)

        val result = runBlocking {
            proxyApi.post(proxyRequest)
        }

        expectThat(
            (result as List<Any?>).filterNotNull().all { it is ElementValueReference }).isTrue()
    }

    @Test
    fun `should transform array list proxy response to element value references`() {
        val responseJson = """["foo", null, "bar", null, "yaz", "qux"]"""

        setupMocks(responseJson)

        val result = runBlocking {
            proxyApi.post(proxyRequest)
        }

        expectThat(
            (result as List<Any?>).filterNotNull().all { it is ElementValueReference }).isTrue()
    }

    private fun proxyMethodsTestsInput(): Array<Any?> {
        return arrayOf(
            arrayOf(HttpMethod.GET, null, null, null),
            arrayOf(HttpMethod.DELETE, null, null, null),
            arrayOf(HttpMethod.POST, "text", "plain", "Hello World"),
            arrayOf(HttpMethod.PATCH, "text", "plain", "Hello World"),
            arrayOf(HttpMethod.PUT, "text", "plain", "Hello World")
        )
    }
}