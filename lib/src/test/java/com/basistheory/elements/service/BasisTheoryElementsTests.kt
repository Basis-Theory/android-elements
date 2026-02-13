package com.basistheory.elements.service

import android.app.Activity
import android.view.View
import com.basistheory.elements.constants.ElementValueType
import com.basistheory.elements.model.CreateTokenRequest
import com.basistheory.elements.model.CreateTokenIntentRequest
import com.basistheory.elements.model.UpdateTokenRequest
import com.basistheory.elements.model.ElementValueReference
import com.basistheory.elements.model.EncryptTokenRequest
import com.basistheory.elements.model.EncryptTokenResponse
import com.basistheory.elements.model.exceptions.ApiException
import com.basistheory.elements.model.exceptions.EncryptTokenException
import com.basistheory.elements.model.exceptions.IncompleteElementException
import com.basistheory.elements.model.toJava
import com.basistheory.elements.view.CardExpirationDateElement
import com.basistheory.elements.view.CardNumberElement
import com.basistheory.elements.view.CardVerificationCodeElement
import com.basistheory.elements.view.TextElement
import com.basistheory.resources.sessions.SessionsClient
import com.basistheory.resources.tokenintents.TokenIntentsClient
import com.basistheory.resources.tokens.TokensClient
import com.github.javafaker.Faker
import com.nimbusds.jose.JOSEException
import io.mockk.Called
import io.mockk.CapturingSlot
import io.mockk.slot
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNull
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject

@Config(sdk = [33]) // TODO remove once Roboelectric releases a new version supporting SDK 34 https://github.com/robolectric/robolectric/issues/8404
@RunWith(RobolectricTestRunner::class)
class BasisTheoryElementsTests {
    private val faker = Faker()
    private lateinit var nameElement: TextElement
    private lateinit var phoneNumberElement: TextElement
    private lateinit var cardNumberElement: CardNumberElement
    private lateinit var cardExpElement: CardExpirationDateElement
    private lateinit var cvcElement: CardVerificationCodeElement

    private lateinit var textElement: TextElement
    private lateinit var intElement: TextElement
    private lateinit var doubleElement: TextElement
    private lateinit var boolElement: TextElement

    // faker's test card numbers are not all considered complete by our elements; use these in tests below
    private val testCardNumbers = listOf(
        "4242424242424242",
        "4000056655665556",
        "5555555555554444",
        "2223003122003222",
        "5200828282828210",
        "5105105105105100",
        "378282246310005",
        "371449635398431",
        "6011111111111117",
        "6011000990139424",
        "3056930009020004",
        "36227206271667",
        "3566002020360505",
        "6200000000000005"
    )

    @get:Rule
    val mockkRule = MockKRule(this)


    @RelaxedMockK
    private lateinit var tokensApi: TokensClient

    @RelaxedMockK
    private lateinit var tokenIntentsApi: TokenIntentsClient

    @RelaxedMockK
    private lateinit var sessionsApi: SessionsClient

    @RelaxedMockK
    private lateinit var proxyApi: ProxyApi

    @RelaxedMockK
    private lateinit var provider: ApiClientProvider

    @Inject
    private val dispatcher = Dispatchers.Unconfined

    @InjectMockKs
    private lateinit var bt: BasisTheoryElements


    @MockK
    private lateinit var mockHttpClient: OkHttpClient

    @MockK
    private lateinit var mockCall: Call

    @MockK
    private lateinit var mockResponse: Response

    @MockK
    private lateinit var mockResponseBody: ResponseBody

    private lateinit var testProxyApi: ProxyApi

    private var proxyRequest: ProxyRequest = ProxyRequest()

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()

        nameElement = TextElement(activity).also { it.id = View.generateViewId() }
        phoneNumberElement = TextElement(activity).also { it.id = View.generateViewId() }
        cardNumberElement = CardNumberElement(activity).also { it.id = View.generateViewId() }
        cardExpElement = CardExpirationDateElement(activity).also { it.id = View.generateViewId() }
        cvcElement = CardVerificationCodeElement(activity).also { it.id = View.generateViewId() }

        textElement = TextElement(activity).also { it.id = View.generateViewId() }
        intElement = TextElement(activity).also { it.id = View.generateViewId() }
        doubleElement = TextElement(activity).also { it.id = View.generateViewId() }
        boolElement = TextElement(activity).also { it.id = View.generateViewId() }

        testProxyApi = ProxyApi(
            dispatcher = Dispatchers.IO,
            apiBaseUrl = "https://api.flock-dev.com",
            apiKey = "123",
            httpClient = mockHttpClient
        )

        every { mockHttpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse
        every { mockResponse.body } returns mockResponseBody
        every { mockResponseBody.string() } returns "\"Hello World\""
        every { mockResponse.header(any()) } returns null
    }

    @Test
    fun `tokenize should pass api key override to ApiClientProvider`() = runBlocking {
        val apiKeyOverride = UUID.randomUUID().toString()

        every { provider.getTokensApi(any()) } returns tokensApi

        bt.tokenize(object {}, apiKeyOverride)

        verify { provider.getTokensApi(apiKeyOverride) }
    }

    @Test
    fun `tokenize should forward top level primitive value without modification`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            val name = faker.name().fullName()
            bt.tokenize(name)

            verify { tokensApi.tokenize(name) }
        }

    @Test
    fun `tokenize should forward primitive data values within request without modification`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            val request = object {
                val type = "token"
                val data = "primitive"
            }

            bt.tokenize(request)

            val expectedRequest = mapOf<String, Any?>(
                "type" to request.type,
                "data" to request.data
            )

            verify { tokensApi.tokenize(expectedRequest) }
        }

    @Test
    fun `tokenize should forward complex data values within request without modification`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            val request = object {
                val type = "token"
                val data = object {
                    val string = faker.lorem().word()
                    val int = faker.random().nextInt(10, 100)
                    val nullValue = null
                    val nested = object {
                        val double = faker.random().nextDouble()
                        val bool = faker.random().nextBoolean()
                        val timestamp = Instant.now().toString()
                        val nullValue = null
                    }
                    val array = arrayOf<Any?>(
                        faker.lorem().word(),
                        faker.random().nextDouble(),
                        faker.random().nextBoolean(),
                        null
                    )
                }
                val containers = arrayOf(
                    "/test/1/",
                    "/test/2/"
                )
            }

            bt.tokenize(request)

            val expectedRequest = mapOf<String, Any?>(
                "type" to request.type,
                "data" to mapOf(
                    "string" to request.data.string,
                    "int" to request.data.int,
                    "nullValue" to null,
                    "nested" to mapOf(
                        "double" to request.data.nested.double,
                        "bool" to request.data.nested.bool,
                        "timestamp" to request.data.nested.timestamp,
                        "nullValue" to null,
                    ),
                    "array" to arrayListOf(
                        request.data.array[0],
                        request.data.array[1],
                        request.data.array[2],
                        null
                    )
                ),
                "containers" to arrayListOf(
                    request.containers[0],
                    request.containers[1]
                )
            )

            verify { tokensApi.tokenize(expectedRequest) }
        }

    @Test
    fun `tokenize should replace top level TextElement ref with underlying data value`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            val name = faker.name().fullName()
            nameElement.setText(name)

            bt.tokenize(nameElement)

            verify { tokensApi.tokenize(name) }
        }

    @Test
    fun `tokenize should replace top level CardElement ref with underlying data value`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            val cardNumber = testCardNumbers.random()
            cardNumberElement.setText(cardNumber)

            bt.tokenize(cardNumberElement)

            val expectedTokenizedCardNumber = cardNumber.replace(Regex("""[^\d]"""), "")
            verify { tokensApi.tokenize(expectedTokenizedCardNumber) }
        }

    @Test
    fun `tokenize should replace top level CardExpirationDateElement refs with underlying data value`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            val expDate = LocalDate.now().plus(2, ChronoUnit.YEARS)
            val month = expDate.monthValue.toString().padStart(2, '0')
            val year = expDate.year.toString()
            cardExpElement.setText("$month/${year.takeLast(2)}")

            bt.tokenize(cardExpElement.month())
            verify { tokensApi.tokenize(expDate.monthValue) }

            bt.tokenize(cardExpElement.year())
            verify { tokensApi.tokenize(expDate.year) }

            bt.tokenize(cardExpElement.format("MM"))
            verify { tokensApi.tokenize(month) }

            bt.tokenize(cardExpElement.format("yyyy"))
            verify { tokensApi.tokenize(year) }

            if (month.take(1) == "0") {
                bt.tokenize(cardExpElement.format("M"))
                verify { tokensApi.tokenize(month.takeLast(1)) }
            } else {
                bt.tokenize(cardExpElement.format("M"))
                verify { tokensApi.tokenize(month) }
            }

            bt.tokenize(cardExpElement.format("yyyyMM"))
            verify { tokensApi.tokenize(year + month) }

            bt.tokenize(cardExpElement.format("MM/yyyy"))
            verify { tokensApi.tokenize("$month/$year") }

            bt.tokenize(cardExpElement.format("MM/yy"))
            verify { tokensApi.tokenize("$month/${year.takeLast(2)}") }

            bt.tokenize(cardExpElement.format("MM-yyyy"))
            verify { tokensApi.tokenize("$month-$year") }
        }

    @Test
    fun `tokenize should replace Element refs within request object with underlying data values`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            val name = faker.name().fullName()
            nameElement.setText(name)

            val phoneNumber = faker.phoneNumber().phoneNumber()
            phoneNumberElement.setText(phoneNumber)

            val cardNumber = testCardNumbers.random()
            cardNumberElement.setText(cardNumber)

            val expDate = LocalDate.now().plus(2, ChronoUnit.YEARS)
            val expMonth = expDate.monthValue.toString().padStart(2, '0')
            val expYear = expDate.year.toString()
            cardExpElement.setText("$expMonth/${expYear.takeLast(2)}")

            val cvc = faker.random().nextInt(100, 999).toString()
            cvcElement.setText(cvc)

            val request = object {
                val type = "token"
                val data = object {
                    val raw = faker.lorem().word()
                    val name = nameElement
                    val card = object {
                        val number = cardNumberElement
                        val expMonth = cardExpElement.month()
                        val expYear = cardExpElement.year()
                        val cvc = cvcElement
                    }
                    val nested = object {
                        val raw = faker.lorem().word()
                        val phoneNumber = phoneNumberElement
                    }
                    val array = arrayOf(
                        nameElement,
                        phoneNumberElement,
                        null
                    )
                    val arrayList = arrayListOf(
                        nameElement,
                        phoneNumberElement,
                        null
                    )
                }
            }

            bt.tokenize(request)

            val expectedRequest = mapOf<String, Any?>(
                "type" to request.type,
                "data" to mapOf(
                    "raw" to request.data.raw,
                    "name" to name,
                    "card" to mapOf(
                        "number" to cardNumber.replace(Regex("""[^\d]"""), ""),
                        "expMonth" to expDate.monthValue,
                        "expYear" to expDate.year,
                        "cvc" to cvc
                    ),
                    "nested" to mapOf(
                        "raw" to request.data.nested.raw,
                        "phoneNumber" to phoneNumber
                    ),
                    "array" to arrayListOf(
                        name,
                        phoneNumber,
                        null
                    ),
                    "arrayList" to arrayListOf(
                        name,
                        phoneNumber,
                        null
                    )
                )
            )

            verify { tokensApi.tokenize(expectedRequest) }
        }

    @Test
    fun `tokenize should respect getValueType type when sending values to the API`() = runBlocking {
        every { provider.getTokensApi(any()) } returns tokensApi

        val testString = faker.name().firstName()
        val testInt = faker.number().numberBetween(1, 10)
        val testDouble = faker.number().randomDouble(2, 10, 99)
        val testBoolean = faker.bool().bool()

        // individual
        textElement.setText(testString)
        bt.tokenize(textElement)
        verify { tokensApi.tokenize(testString) }

        intElement.setText(testInt.toString())
        intElement.getValueType = ElementValueType.INTEGER
        bt.tokenize(intElement)
        verify { tokensApi.tokenize(testInt) }

        doubleElement.setText(testDouble.toString())
        doubleElement.getValueType = ElementValueType.DOUBLE
        bt.tokenize(doubleElement)
        verify { tokensApi.tokenize(testDouble) }

        boolElement.setText(testBoolean.toString())
        boolElement.getValueType = ElementValueType.BOOLEAN
        bt.tokenize(boolElement)
        verify { tokensApi.tokenize(testBoolean) }

        // grouped
        val request = object {
            val type = "token"
            val data = object {
                val text = textElement
                val int = intElement
                val double = doubleElement
                val bool = boolElement
            }
        }

        bt.tokenize(request)

        val expectedRequest = mapOf<String, Any?>(
            "type" to request.type,
            "data" to mapOf(
                "text" to testString,
                "int" to testInt,
                "double" to testDouble,
                "bool" to testBoolean
            )
        )
        verify { tokensApi.tokenize(expectedRequest) }
    }

    @Test
    fun `createToken should pass api key override to ApiClientProvider`() = runBlocking {
        val apiKeyOverride = UUID.randomUUID().toString()

        every { provider.getTokensApi(any()) } returns tokensApi
        every { tokensApi.create(any()) } returns fakeToken()

        bt.createToken(CreateTokenRequest(type = "token", data = ""), apiKeyOverride)

        verify { provider.getTokensApi(apiKeyOverride) }
    }

    @Test
    fun `createToken should forward top level primitive value without modification`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi
            every { tokensApi.create(any()) } returns fakeToken()

            val name = faker.name().fullName()
            val createTokenRequest = createTokenRequest(name)
            bt.createToken(createTokenRequest)

            verify { tokensApi.create(createTokenRequest.toJava()) }
        }

    @Test
    fun `createToken should forward complex data values within request without modification`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi
            every { tokensApi.create(any()) } returns fakeToken()

            val data = object {
                val string = faker.lorem().word()
                val int = faker.random().nextInt(10, 100)
                val nullValue = null
                val nested = object {
                    val double = faker.random().nextDouble()
                    val bool = faker.random().nextBoolean()
                    val timestamp = Instant.now().toString()
                    val nullValue = null
                }
                val array = arrayOf<Any?>(
                    faker.lorem().word(),
                    faker.random().nextDouble(),
                    faker.random().nextBoolean(),
                    null
                )
            }
            val request = createTokenRequest(data)

            bt.createToken(request)

            val expectedData = mapOf(
                "string" to data.string,
                "int" to data.int,
                "nullValue" to null,
                "nested" to mapOf(
                    "double" to data.nested.double,
                    "bool" to data.nested.bool,
                    "timestamp" to data.nested.timestamp,
                    "nullValue" to null
                ),
                "array" to arrayListOf(
                    data.array[0],
                    data.array[1],
                    data.array[2],
                    null
                )
            )
            val expectedRequest = createTokenRequest(expectedData)

            verify { tokensApi.create(expectedRequest.toJava()) }
        }

    @Test
    fun `createToken should replace top level TextElement ref with underlying data value`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi
            every { tokensApi.create(any()) } returns fakeToken()

            val name = faker.name().fullName()
            nameElement.setText(name)

            val createTokenRequest = createTokenRequest(nameElement)

            bt.createToken(createTokenRequest)

            val expectedRequest = createTokenRequest(name)

            verify { tokensApi.create(expectedRequest.toJava()) }
        }

    @Test
    fun `createToken should replace top level CardElement ref with underlying data value`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi
            every { tokensApi.create(any()) } returns fakeToken()

            val cardNumber = testCardNumbers.random()
            cardNumberElement.setText(cardNumber)

            val createTokenRequest = createTokenRequest(cardNumberElement)

            bt.createToken(createTokenRequest)

            val expectedRequest = createTokenRequest(cardNumber.replace(Regex("""[^\d]"""), ""))

            verify { tokensApi.create(expectedRequest.toJava()) }
        }

    @Test
    fun `createToken should replace top level CardExpirationDateElement refs with underlying data value`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi
            every { tokensApi.create(any()) } returns fakeToken()

            val expDate = LocalDate.now().plus(2, ChronoUnit.YEARS)
            val month = expDate.monthValue.toString().padStart(2, '0')
            val year = expDate.year.toString()
            cardExpElement.setText("$month/${year.takeLast(2)}")

            val createTokenRequestMonth = createTokenRequest(cardExpElement.month())
            val createTokenRequestYear = createTokenRequest(cardExpElement.year())

            bt.createToken(createTokenRequestMonth)

            val expectedMonthRequest = createTokenRequest(expDate.monthValue)
            verify { tokensApi.create(expectedMonthRequest.toJava()) }

            bt.createToken(createTokenRequestYear)

            val expectedYearRequest = createTokenRequest(expDate.year)
            verify { tokensApi.create(expectedYearRequest.toJava()) }
        }

    @Test
    fun `createToken should replace Element refs within request object with underlying data values`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi
            every { tokensApi.create(any()) } returns fakeToken()

            val name = faker.name().fullName()
            nameElement.setText(name)

            val phoneNumber = faker.phoneNumber().phoneNumber()
            phoneNumberElement.setText(phoneNumber)

            val cardNumber = testCardNumbers.random()
            cardNumberElement.setText(cardNumber)

            val expDate = LocalDate.now().plus(2, ChronoUnit.YEARS)
            val expMonth = expDate.monthValue.toString().padStart(2, '0')
            val expYear = expDate.year.toString()
            cardExpElement.setText("$expMonth/${expYear.takeLast(2)}")

            val cvc = faker.random().nextInt(100, 999).toString()
            cvcElement.setText(cvc)

            val data = object {
                val type = "token"
                val data = object {
                    val raw = faker.lorem().word()
                    val name = nameElement
                    val card = object {
                        val number = cardNumberElement
                        val expMonth = cardExpElement.month()
                        val expYear = cardExpElement.year()
                        val cvc = cvcElement
                    }
                    val nested = object {
                        val raw = faker.lorem().word()
                        val phoneNumber = phoneNumberElement
                    }
                    val array = arrayOf(
                        nameElement,
                        phoneNumberElement,
                        null
                    )
                }
            }
            val createTokenRequest = createTokenRequest(data)

            bt.createToken(createTokenRequest)

            val expectedData = mapOf<String, Any?>(
                "type" to data.type,
                "data" to mapOf(
                    "raw" to data.data.raw,
                    "name" to name,
                    "card" to mapOf(
                        "number" to cardNumber.replace(Regex("""[^\d]"""), ""),
                        "expMonth" to expDate.monthValue,
                        "expYear" to expDate.year,
                        "cvc" to cvc
                    ),
                    "nested" to mapOf(
                        "raw" to data.data.nested.raw,
                        "phoneNumber" to phoneNumber
                    ),
                    "array" to arrayListOf(
                        name,
                        phoneNumber,
                        null
                    )
                )
            )

            val expectedCreateTokenRequest = createTokenRequest(expectedData)

            verify { tokensApi.create(expectedCreateTokenRequest.toJava()) }
        }

    @Test
    fun `createToken should respect getValueType type when sending values to the API`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi
            every { tokensApi.create(any()) } returns fakeToken()


            val testString = faker.name().firstName()
            val testInt = faker.number().numberBetween(1, 10)
            val testDouble = faker.number().randomDouble(2, 10, 99)
            val testBoolean = faker.bool().bool()

            textElement.setText(testString)

            intElement.setText(testInt.toString())
            intElement.getValueType = ElementValueType.INTEGER

            doubleElement.setText(testDouble.toString())
            doubleElement.getValueType = ElementValueType.DOUBLE

            boolElement.setText(testBoolean.toString())
            boolElement.getValueType = ElementValueType.BOOLEAN

            val request = object {
                val type = "token"
                val data = object {
                    val text = textElement
                    val int = intElement
                    val double = doubleElement
                    val bool = boolElement
                }
            }

            val createTokenRequest = createTokenRequest(request)

            bt.createToken(createTokenRequest)

            val expectedRequest = mapOf<String, Any?>(
                "type" to request.type,
                "data" to mapOf(
                    "text" to testString,
                    "int" to testInt,
                    "double" to testDouble,
                    "bool" to testBoolean
                )
            )

            val expectedCreateTokenRequest = createTokenRequest(expectedRequest)

            verify { tokensApi.create(expectedCreateTokenRequest.toJava()) }
        }

    @Test
    fun `createTokenIntent should pass api key override to ApiClientProvider`() = runBlocking {
        val apiKeyOverride = UUID.randomUUID().toString()

        every { provider.getTokenIntentsApi(any()) } returns tokenIntentsApi
        every { tokenIntentsApi.create(any()) } returns fakeTokenIntent()

        bt.createTokenIntent(CreateTokenIntentRequest(type = "card", data = ""), apiKeyOverride)

        verify { provider.getTokenIntentsApi(apiKeyOverride) }
    }

    @Test
    fun `createTokenIntent should forward top level primitive value without modification`() =
        runBlocking {
            every { provider.getTokenIntentsApi(any()) } returns tokenIntentsApi
            every { tokenIntentsApi.create(any()) } returns fakeTokenIntent()

            val name = faker.name().fullName()
            val createTokenIntentRequest = createTokenIntentRequest(name)
            bt.createTokenIntent(createTokenIntentRequest)

            verify { tokenIntentsApi.create(createTokenIntentRequest.toJava()) }
        }

    @Test
    fun `createTokenIntent should forward complex data values within request without modification`() =
        runBlocking {
            every { provider.getTokenIntentsApi(any()) } returns tokenIntentsApi
            every { tokenIntentsApi.create(any()) } returns fakeTokenIntent()

            val data = object {
                val string = faker.lorem().word()
                val int = faker.random().nextInt(10, 100)
                val nullValue = null
                val nested = object {
                    val double = faker.random().nextDouble()
                    val bool = faker.random().nextBoolean()
                    val timestamp = Instant.now().toString()
                    val nullValue = null
                }
                val array = arrayOf<Any?>(
                    faker.lorem().word(),
                    faker.random().nextDouble(),
                    faker.random().nextBoolean(),
                    null
                )
            }
            val request = createTokenIntentRequest(data)

            bt.createTokenIntent(request)

            val expectedData = mapOf(
                "string" to data.string,
                "int" to data.int,
                "nullValue" to null,
                "nested" to mapOf(
                    "double" to data.nested.double,
                    "bool" to data.nested.bool,
                    "timestamp" to data.nested.timestamp,
                    "nullValue" to null
                ),
                "array" to arrayListOf(
                    data.array[0],
                    data.array[1],
                    data.array[2],
                    null
                )
            )
            val expectedRequest = createTokenIntentRequest(expectedData)

            verify { tokenIntentsApi.create(expectedRequest.toJava()) }
        }

    @Test
    fun `createTokenIntent should replace top level TextElement ref with underlying data value`() =
        runBlocking {
            every { provider.getTokenIntentsApi(any()) } returns tokenIntentsApi
            every { tokenIntentsApi.create(any()) } returns fakeTokenIntent()

            val name = faker.name().fullName()
            nameElement.setText(name)

            val createTokenIntentRequest = createTokenIntentRequest(nameElement)

            bt.createTokenIntent(createTokenIntentRequest)

            val expectedRequest = createTokenIntentRequest(name)

            verify { tokenIntentsApi.create(expectedRequest.toJava()) }
        }

    @Test
    fun `createTokenIntent should replace Element refs within request object with underlying data values`() =
        runBlocking {
            every { provider.getTokenIntentsApi(any()) } returns tokenIntentsApi
            every { tokenIntentsApi.create(any()) } returns fakeTokenIntent()

            val name = faker.name().fullName()
            nameElement.setText(name)

            val cardNumber = testCardNumbers.random()
            cardNumberElement.setText(cardNumber)

            val expDate = LocalDate.now().plus(2, ChronoUnit.YEARS)
            val expMonth = expDate.monthValue.toString().padStart(2, '0')
            val expYear = expDate.year.toString()
            cardExpElement.setText("$expMonth/${expYear.takeLast(2)}")

            val cvc = faker.random().nextInt(100, 999).toString()
            cvcElement.setText(cvc)

            val data = object {
                val type = "card"
                val data = object {
                    val name = nameElement
                    val number = cardNumberElement
                    val expiration_month = cardExpElement.month()
                    val expiration_year = cardExpElement.year()
                    val cvc = cvcElement
                }
            }
            val createTokenIntentRequest = createTokenIntentRequest(data)

            bt.createTokenIntent(createTokenIntentRequest)

            val expectedData = mapOf<String, Any?>(
                "type" to data.type,
                "data" to mapOf(
                    "name" to name,
                    "number" to cardNumber.replace(Regex("""[^\d]"""), ""),
                    "expiration_month" to expDate.monthValue,
                    "expiration_year" to expDate.year,
                    "cvc" to cvc
                )
            )

            val expectedCreateTokenIntentRequest = createTokenIntentRequest(expectedData)

            verify { tokenIntentsApi.create(expectedCreateTokenIntentRequest.toJava()) }
        }

    @Test
    fun `createTokenIntent throws ApiException when an exception occurs`(): Unit =
        runBlocking {
            every { provider.getTokenIntentsApi(any()) } returns tokenIntentsApi

            every { tokenIntentsApi.create(any()) } throws com.basistheory.core.BasisTheoryApiApiException(
                "Api Error",
                401,
                ""
            )

            val createTokenIntentRequest = CreateTokenIntentRequest(type = "card", data = "")

            expectCatching {
                bt.createTokenIntent(
                    createTokenIntentRequest,
                    apiKeyOverride = faker.name().firstName()
                )
            }
                .isFailure()
                .isA<ApiException>().and {
                    get { code }.isEqualTo(401)
                }
        }

    @Test
    fun `proxy should replace Element refs within request object with underlying data values`() {
        val name = faker.name().fullName()
        nameElement.setText(name)

        val phoneNumber = faker.phoneNumber().phoneNumber()
        phoneNumberElement.setText(phoneNumber)


        var data = object {
            val name = nameElement
            val phone = phoneNumberElement
        }

        val stringifiedData = "{\"name\":\"${name}\",\"phone\":\"${phoneNumber}\"}"

        proxyRequest = proxyRequest.apply {
            headers = mapOf(
                "BT-PROXY-URL" to "https://echo.basistheory.com/post",
                "Content-Type" to "application/json"
            )
            body = data
        }

        val requestSlot = setupProxyMocks()
        val result = runBlocking {
            testProxyApi.post(proxyRequest)
        }

        verify(exactly = 1) { mockHttpClient.newCall(any()) }

        expectThat(requestSlot.captured) {
            get { headers["BT-PROXY-URL"] }.isEqualTo("https://echo.basistheory.com/post")
            get { body?.contentType()?.type }.isEqualTo("application")
            get { body?.contentType()?.subtype }.isEqualTo("json")

            if (this.subject.body != null) {
                val buffer = Buffer()
                this.subject.body!!.writeTo(buffer)
                val bodyInRequest = buffer.readUtf8()
                expectThat(bodyInRequest).isEqualTo(stringifiedData)
            } else {
                get { body }.isNull()
            }
        }

        expectThat(result).isA<ElementValueReference>()
    }

    @Test
    fun `proxy should replace top level TextElement ref with underlying data value`() {
        val name = faker.name().fullName()
        nameElement.setText(name)

        proxyRequest = proxyRequest.apply {
            headers = mapOf(
                "BT-PROXY-URL" to "https://echo.basistheory.com/post",
                "Content-Type" to "text/plain"
            )
            body = nameElement
        }

        val requestSlot = setupProxyMocks()

        val result = runBlocking {
            testProxyApi.post(proxyRequest)
        }

        verify(exactly = 1) { mockHttpClient.newCall(any()) }

        expectThat(requestSlot.captured) {
            get { headers["BT-PROXY-URL"] }.isEqualTo("https://echo.basistheory.com/post")
            get { body?.contentType()?.type }.isEqualTo("text")
            get { body?.contentType()?.subtype }.isEqualTo("plain")

            if (this.subject.body != null) {
                val buffer = Buffer()
                this.subject.body!!.writeTo(buffer)
                val bodyInRequest = buffer.readUtf8()
                expectThat(bodyInRequest).isEqualTo(name)
            } else {
                get { body }.isNull()
            }
        }

        expectThat(result).isA<ElementValueReference>()
    }

    @Test
    fun `proxy should replace top level CardNumberElement ref with underlying data value`() {
        val cardNumber = testCardNumbers.random()
        cardNumberElement.setText(cardNumber)

        proxyRequest = proxyRequest.apply {
            headers = mapOf(
                "BT-PROXY-URL" to "https://echo.basistheory.com/post",
                "Content-Type" to "text/plain"
            )
            body = cardNumberElement
        }

        val requestSlot = setupProxyMocks()

        val result = runBlocking {
            testProxyApi.post(proxyRequest)
        }

        verify(exactly = 1) { mockHttpClient.newCall(any()) }

        expectThat(requestSlot.captured) {
            get { headers["BT-PROXY-URL"] }.isEqualTo("https://echo.basistheory.com/post")
            get { body?.contentType()?.type }.isEqualTo("text")
            get { body?.contentType()?.subtype }.isEqualTo("plain")

            if (this.subject.body != null) {
                val buffer = Buffer()
                this.subject.body!!.writeTo(buffer)
                val bodyInRequest = buffer.readUtf8()
                expectThat(bodyInRequest).isEqualTo(cardNumber.replace(Regex("""[^\d]"""), ""))
            } else {
                get { body }.isNull()
            }
        }

        expectThat(result).isA<ElementValueReference>()
    }

    @Test
    fun `proxy should replace top level CardExpirationDateElement ref with underlying data value`() {
        val expDate = LocalDate.now().plus(2, ChronoUnit.YEARS)
        val month = expDate.monthValue.toString().padStart(2, '0')
        val year = expDate.year.toString()
        val expDateString = "$month/${year.takeLast(2)}"
        cardExpElement.setText(expDateString)

        proxyRequest = proxyRequest.apply {
            headers = mapOf(
                "BT-PROXY-URL" to "https://echo.basistheory.com/post",
                "Content-Type" to "text/plain"
            )
            body = cardExpElement
        }

        val requestSlot = setupProxyMocks()

        val result = runBlocking {
            testProxyApi.post(proxyRequest)
        }

        verify(exactly = 1) { mockHttpClient.newCall(any()) }


        expectThat(requestSlot.captured) {
            get { headers["BT-PROXY-URL"] }.isEqualTo("https://echo.basistheory.com/post")
            get { body?.contentType()?.type }.isEqualTo("text")
            get { body?.contentType()?.subtype }.isEqualTo("plain")

            if (this.subject.body != null) {
                val buffer = Buffer()
                this.subject.body!!.writeTo(buffer)
                val bodyInRequest = buffer.readUtf8()
                expectThat(bodyInRequest).isEqualTo(expDateString)
            } else {
                get { body }.isNull()
            }
        }

        expectThat(result).isA<ElementValueReference>()
    }

    // note: junit only supports one @RunWith class per test class, so we can't use JUnitParamsRunner here
    @Test
    fun `throws IncompleteElementException when attempting to tokenize luhn-invalid card`() =
        incompleteCardThrowsIncompleteElementException("4242424242424245")

    @Test
    fun `throws IncompleteElementException when attempting to tokenize partial card`() =
        incompleteCardThrowsIncompleteElementException("424242")

    @Test
    fun `throws IncompleteElementException when attempting to tokenize invalid expiration dates`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            cardExpElement.setText("11/01")

            val createTokenRequest = createTokenRequest(object {
                val expirationMonth = cardExpElement.month()
                val expirationYear = cardExpElement.year()
            })

            expectCatching { bt.createToken(createTokenRequest) }
                .isFailure()
                .isA<IncompleteElementException>().and {
                    get { message }.isEqualTo(
                        IncompleteElementException.errorMessageFor(
                            cardExpElement.id
                        )
                    )
                }

            verify { tokensApi.create(any()) wasNot Called }
        }

    @Test
    fun `tokenize throws com_basistheory_android_model_exceptions_ApiException when an exception occurs`(): Unit =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            every { tokensApi.tokenize(any()) } throws com.basistheory.core.BasisTheoryApiApiException(
                "Api Error",
                401,
                ""
            )

            val name = faker.name().fullName()

            expectCatching { bt.tokenize(name, apiKeyOverride = name) }
                .isFailure()
                .isA<ApiException>().and {
                    get { code }.isEqualTo(401)
                }
        }

    @Test
    fun `createToken throws com_basistheory_android_model_exceptions_ApiException when an exception occurs`(): Unit =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            every { tokensApi.create(any()) } throws com.basistheory.core.BasisTheoryApiApiException(
                "Api Error",
                401,
                ""
            )

            val createTokenRequest = CreateTokenRequest(type = "token", data = "")

            expectCatching {
                bt.createToken(
                    createTokenRequest,
                    apiKeyOverride = faker.name().firstName()
                )
            }
                .isFailure()
                .isA<ApiException>().and {
                    get { code }.isEqualTo(401)
                }
        }

    @Test
    fun `getToken throws com_basistheory_android_model_exceptions_ApiException when an exception occurs`(): Unit =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            every { tokensApi.get(any()) } throws com.basistheory.core.BasisTheoryApiApiException(
                "Api Error",
                401,
                ""
            )

            expectCatching {
                bt.getToken(
                    UUID.randomUUID().toString(),
                    apiKeyOverride = faker.name().firstName()
                )
            }
                .isFailure()
                .isA<ApiException>().and {
                    get { code }.isEqualTo(401)
                }
        }

    @Test
    fun `createSession throws com_basistheory_android_model_exceptions_ApiException when an exception occurs`(): Unit =
        runBlocking {
            every { provider.getSessionsApi(any()) } returns sessionsApi

            every { sessionsApi.create() } throws com.basistheory.core.BasisTheoryApiApiException(
                "Api Error",
                401,
                ""
            )

            expectCatching { bt.createSession(apiKeyOverride = faker.name().firstName()) }
                .isFailure()
                .isA<ApiException>().and {
                    get { code }.isEqualTo(401)
                }
        }

    @Test
    fun `createSession should call java SDK without api key override`() = runBlocking {
        every { provider.getSessionsApi(any()) } returns sessionsApi
        every { sessionsApi.create() } returns fakeSession()

        bt.createSession()

        verify { provider.getSessionsApi() }
        verify { sessionsApi.create() }
    }

    @Test
    fun `createSession should call java SDK with api key override`() = runBlocking {
        val apiKeyOverride = UUID.randomUUID().toString()
        every { provider.getSessionsApi(any()) } returns sessionsApi
        every { sessionsApi.create() } returns fakeSession()

        bt.createSession(apiKeyOverride)

        verify { provider.getSessionsApi(apiKeyOverride) }
        verify { sessionsApi.create() }
    }

    @Test
    fun `getToken should call java SDK without api key override`() = runBlocking {
        val tokenId = UUID.randomUUID().toString()

        every { provider.getTokensApi(any()) } returns tokensApi
        every { tokensApi.get(tokenId) } returns fakeToken()

        bt.getToken(tokenId)

        verify { provider.getTokensApi() }
        verify { tokensApi.get(tokenId) }
    }

    @Test
    fun `getToken should call java SDK with api key override`() = runBlocking {
        val tokenId = UUID.randomUUID().toString()
        val apiKeyOverride = UUID.randomUUID().toString()

        every { provider.getTokensApi(any()) } returns tokensApi
        every { tokensApi.get(tokenId) } returns fakeToken()

        bt.getToken(tokenId, apiKeyOverride)

        verify { provider.getTokensApi(apiKeyOverride) }
        verify { tokensApi.get(tokenId) }
    }

    @Test
    fun `provides a proxy instance`() = runBlocking {
        every { provider.getProxyApi(any()) } returns proxyApi

        expectThat(bt.proxy).isNotEqualTo(null)

        verify { provider.getProxyApi(any()) }
    }

    @Test
    fun `encryptTokens should handle single token request with elements`() {
        val cardNumber = testCardNumbers.random()
        cardNumberElement.setText(cardNumber)

        val expDate = LocalDate.now().plus(2, ChronoUnit.YEARS)
        val expMonth = expDate.monthValue.toString().padStart(2, '0')
        val expYear = expDate.year.toString()
        cardExpElement.setText("$expMonth/${expYear.takeLast(2)}")

        val cvc = faker.random().nextInt(100, 999).toString()
        cvcElement.setText(cvc)

        val encryptTokenRequest = EncryptTokenRequest(
            tokenRequests = object {
                val data = object {
                    val number = cardNumberElement
                    val expiration_month = cardExpElement.month()
                    val expiration_year = cardExpElement.year()
                    val cvc = cvcElement
                }
                val type = "card"
            },
            publicKey = "-----BEGIN PUBLIC KEY-----\nw6RFs74UmOcxjbWBSlZQ0QLam63bvKQvGeLSVfxYIR8=\n-----END PUBLIC KEY-----",
            keyId = "d6b86549-212f-4bdc-adeb-2f39902740f6"
        )

        val result = bt.encryptToken(encryptTokenRequest)
        expectThat(result).isA<EncryptTokenResponse>().and {
            get { type }.isEqualTo("card")
            get { encrypted }.isNotEmpty()
        }
    }


    @Test
    fun `encryptTokens should handle multiple token requests with elements`() {
        val name = faker.name().fullName()
        nameElement.setText(name)

        val phoneNumber = faker.phoneNumber().phoneNumber()
        phoneNumberElement.setText(phoneNumber)

        val encryptTokenRequest = EncryptTokenRequest(
            tokenRequests = object {
                val tokenA = object {
                    val data = object {
                        val name = nameElement
                        val phoneNumber = phoneNumberElement
                        val note = "Non sensitive value"
                    }
                    val type = "token"
                }
                val tokenB = object {
                    val data = object {
                        val name = nameElement
                        val phoneNumber = phoneNumberElement
                        val note = "Non sensitive value"
                    }
                    val type = "token"
                }
            },
            publicKey = "-----BEGIN PUBLIC KEY-----\nw6RFs74UmOcxjbWBSlZQ0QLam63bvKQvGeLSVfxYIR8=\n-----END PUBLIC KEY-----",
            keyId = "d6b86549-212f-4bdc-adeb-2f39902740f6"
        )

        val result = bt.encryptToken(encryptTokenRequest)
        expectThat(result).isA<Map<String, Any>>()
        
        val resultMap = result as Map<String, Any>
        
        expectThat(resultMap).containsKey("tokenA")
        val tokenA = resultMap["tokenA"] as Map<String, Any>
        expectThat(tokenA).and {
            containsKey("encrypted")
            containsKey("type")
            get { get("type") }.isEqualTo("token")
            get { get("encrypted") }.isA<String>().isNotEqualTo("")
        }
        
        expectThat(resultMap).containsKey("tokenB")
        val tokenB = resultMap["tokenB"] as Map<String, Any>
        expectThat(tokenB).and {
            containsKey("encrypted") 
            containsKey("type")
            get { get("type") }.isEqualTo("token")
            get { get("encrypted") }.isA<String>().isNotEqualTo("")
        }
    }

    @Test
    fun `encryptTokens should throw EncryptTokenException when data is null`() {
        val encryptTokenRequest = EncryptTokenRequest(
            tokenRequests = object {
                val data = null
                val type = "token"
            },
            publicKey = "-----BEGIN PUBLIC KEY-----\nw6RFs74UmOcxjbWBSlZQ0QLam63bvKQvGeLSVfxYIR8=\n-----END PUBLIC KEY-----",
            keyId = "d6b86549-212f-4bdc-adeb-2f39902740f6"
        )

        expectCatching { bt.encryptToken(encryptTokenRequest) }
            .isFailure()
            .isA<IllegalArgumentException>()
            .and {
                get { message }.isEqualTo("Token data must be provided")
            }
    }

    @Test
    fun `encryptTokens should throw EncryptTokenException when type is null`() {
        val encryptTokenRequest = EncryptTokenRequest(
            tokenRequests = object {
                val data = object {
                    val name = faker.name().fullName()
                }
                val type = null
            },
            publicKey = "-----BEGIN PUBLIC KEY-----\nw6RFs74UmOcxjbWBSlZQ0QLam63bvKQvGeLSVfxYIR8=\n-----END PUBLIC KEY-----",
            keyId = "d6b86549-212f-4bdc-adeb-2f39902740f6"
        )

        expectCatching { bt.encryptToken(encryptTokenRequest) }
            .isFailure()
            .isA<IllegalArgumentException>()
            .and {
                get { message }.isEqualTo("Token type must be provided")
            }
    }

    @Test
    fun `encryptTokens should throw EncryptTokenException when publicKey is invalid`() {
        val encryptTokenRequest = EncryptTokenRequest(
            tokenRequests = object {
                val data = object {
                    val name = faker.name().fullName()
                }
                val type = "token"
            },
            publicKey = "",
            keyId = "d6b86549-212f-4bdc-adeb-2f39902740f6"
        )

        expectCatching { bt.encryptToken(encryptTokenRequest) }
            .isFailure()
            .isA<EncryptTokenException>()
            .and {
                get { cause }.isA<JOSEException>().and {
                    get { message }.isEqualTo("Public key length is not 32-byte")
                }
            }
    }

    @Test
    fun `encryptTokens should throw EncryptTokenException when keyId is invalid`() {
        val encryptTokenRequest = EncryptTokenRequest(
            tokenRequests = object {
                val data = object {
                    val name = faker.name().fullName()
                }
                val type = "token"
            },
            publicKey = "-----BEGIN PUBLIC KEY-----\nw6RFs74UmOcxjbWBSlZQ0QLam63bvKQvGeLSVfxYIR8=\n-----END PUBLIC KEY-----",
            keyId = ""
        )

        expectCatching { bt.encryptToken(encryptTokenRequest) }
            .isFailure()
            .isA<IllegalArgumentException>()
            .and {
                get { message }.isEqualTo("Key ID is required")
            }
    }

    @Test
    fun `encryptTokens should handle single token request with plaintext data`() {
        val name = faker.name().fullName()
        val phoneNumber = faker.phoneNumber().phoneNumber()

        val encryptTokenRequest = EncryptTokenRequest(
            tokenRequests = object {
                val data = object {
                    val name = name
                    val phoneNumber = phoneNumber
                    val note = "Non sensitive value"
                }
                val type = "token"
            },
            publicKey = "-----BEGIN PUBLIC KEY-----\nw6RFs74UmOcxjbWBSlZQ0QLam63bvKQvGeLSVfxYIR8=\n-----END PUBLIC KEY-----",
            keyId = "d6b86549-212f-4bdc-adeb-2f39902740f6"
        )

        val result = bt.encryptToken(encryptTokenRequest)
        expectThat(result).isA<EncryptTokenResponse>().and {
            get { type }.isEqualTo("token")
            get { encrypted }.isNotEmpty()
        }
    }

    @Test
    fun `encryptTokens should handle multiple token requests with plaintext data`() {
        val name1 = faker.name().fullName()
        val phoneNumber1 = faker.phoneNumber().phoneNumber()
        val name2 = faker.name().fullName()
        val phoneNumber2 = faker.phoneNumber().phoneNumber()

        val encryptTokenRequest = EncryptTokenRequest(
            tokenRequests = object {
                val tokenA = object {
                    val data = object {
                        val name = name1
                        val phoneNumber = phoneNumber1
                        val note = "Non sensitive value"
                    }
                    val type = "token"
                }
                val tokenB = object {
                    val data = object {
                        val name = name2
                        val phoneNumber = phoneNumber2
                        val note = "Another non sensitive value"
                    }
                    val type = "token"
                }
            },
            publicKey = "-----BEGIN PUBLIC KEY-----\nw6RFs74UmOcxjbWBSlZQ0QLam63bvKQvGeLSVfxYIR8=\n-----END PUBLIC KEY-----",
            keyId = "d6b86549-212f-4bdc-adeb-2f39902740f6"
        )

        val result = bt.encryptToken(encryptTokenRequest)
        expectThat(result).isA<Map<String, Any>>()

        val resultMap = result as Map<String, Any>

        expectThat(resultMap).containsKey("tokenA")
        val tokenA = resultMap["tokenA"] as Map<String, Any>
        expectThat(tokenA).and {
            containsKey("encrypted")
            containsKey("type")
            get { get("type") }.isEqualTo("token")
            get { get("encrypted") }.isA<String>().isNotEqualTo("")
        }

        expectThat(resultMap).containsKey("tokenB")
        val tokenB = resultMap["tokenB"] as Map<String, Any>
        expectThat(tokenB).and {
            containsKey("encrypted")
            containsKey("type")
            get { get("type") }.isEqualTo("token")
            get { get("encrypted") }.isA<String>().isNotEqualTo("")
        }
    }

    @Test
    fun `updateToken should pass api key override to ApiClientProvider`() = runBlocking {
        val apiKeyOverride = UUID.randomUUID().toString()
        val tokenId = UUID.randomUUID().toString()

        every { provider.getTokensApi(any()) } returns tokensApi
        every { tokensApi.update(any(), any()) } returns fakeToken()

        bt.updateToken(tokenId, UpdateTokenRequest(data = ""), apiKeyOverride)

        verify { provider.getTokensApi(apiKeyOverride) }
    }

    @Test
    fun `updateToken should forward top level primitive value without modification`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi
            every { tokensApi.update(any(), any()) } returns fakeToken()

            val tokenId = UUID.randomUUID().toString()
            val name = faker.name().fullName()
            val updateTokenRequest = UpdateTokenRequest(data = name)
            bt.updateToken(tokenId, updateTokenRequest)

            verify { tokensApi.update(tokenId, updateTokenRequest.toJava()) }
        }

    @Test
    fun `updateToken should replace Element refs within request object with underlying data values`() =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi
            every { tokensApi.update(any(), any()) } returns fakeToken()

            val tokenId = UUID.randomUUID().toString()
            val cvc = faker.random().nextInt(100, 999).toString()
            cvcElement.setText(cvc)

            val data = object {
                val cvc = cvcElement
            }
            val updateTokenRequest = UpdateTokenRequest(data = data)

            bt.updateToken(tokenId, updateTokenRequest)

            val expectedData = mapOf<String, Any?>(
                "cvc" to cvc
            )
            val expectedUpdateTokenRequest = UpdateTokenRequest(data = expectedData)

            verify { tokensApi.update(tokenId, expectedUpdateTokenRequest.toJava()) }
        }

    @Test
    fun `updateToken throws ApiException when an exception occurs`(): Unit =
        runBlocking {
            every { provider.getTokensApi(any()) } returns tokensApi

            every { tokensApi.update(any(), any()) } throws com.basistheory.core.BasisTheoryApiApiException(
                "Api Error",
                401,
                ""
            )

            val tokenId = UUID.randomUUID().toString()
            val updateTokenRequest = UpdateTokenRequest(data = "")

            expectCatching {
                bt.updateToken(
                    tokenId,
                    updateTokenRequest,
                    apiKeyOverride = faker.name().firstName()
                )
            }
                .isFailure()
                .isA<ApiException>().and {
                    get { code }.isEqualTo(401)
                }
        }

    private fun createTokenRequest(data: Any): CreateTokenRequest =
        CreateTokenRequest(type = "token", data = data)

    private fun createTokenIntentRequest(data: Any): CreateTokenIntentRequest =
        CreateTokenIntentRequest(type = "card", data = data)

    private fun incompleteCardThrowsIncompleteElementException(
        incompleteCardNumber: String
    ) = runBlocking {
        every { provider.getTokensApi(any()) } returns tokensApi

        cardNumberElement.setText(incompleteCardNumber)

        val createTokenRequest = createTokenRequest(cardNumberElement)

        expectCatching { bt.createToken(createTokenRequest) }
            .isFailure()
            .isA<IncompleteElementException>().and {
                get { message }.isEqualTo(
                    IncompleteElementException.errorMessageFor(
                        cardNumberElement.id
                    )
                )
            }

        verify { tokensApi.create(any()) wasNot Called }
    }

    private fun fakeToken(): com.basistheory.types.Token =
        com.basistheory.types.Token.builder()
            .tenantId(UUID.randomUUID().toString())
            .type("token")
            .data(Faker.instance().name().firstName())
            .createdBy(UUID.randomUUID().toString())
            .createdAt(OffsetDateTime.now())
            .containers(mutableListOf("/general"))
            .build()


    private fun fakeSession(): com.basistheory.types.CreateSessionResponse =
        com.basistheory.types.CreateSessionResponse.builder()
            .sessionKey(UUID.randomUUID().toString())
            .nonce(UUID.randomUUID().toString())
            .expiresAt(OffsetDateTime.now().plusHours(1))
            .build()

    private fun fakeTokenIntent(): com.basistheory.types.CreateTokenIntentResponse =
        com.basistheory.types.CreateTokenIntentResponse.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(UUID.randomUUID().toString())
            .type("card")
            .createdBy(UUID.randomUUID().toString())
            .createdAt(OffsetDateTime.now())
            .expiresAt(OffsetDateTime.now().plusMinutes(15))
            .build()

    private fun setupProxyMocks(requestSlot: CapturingSlot<Request> = slot()):
            CapturingSlot<Request> {
        every { mockHttpClient.newCall(capture(requestSlot)) } returns mockCall
        every { mockCall.execute() } returns mockResponse
        every { mockResponse.body } returns mockResponseBody
        every { mockResponseBody.string() } returns "\"Hello World\""
        every { mockResponse.header(any()) } returns null
        every { mockResponse.close() } just Runs

        return requestSlot
    }
}