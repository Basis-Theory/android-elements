package com.basistheory.android.service

import android.app.Activity
import com.basistheory.TokenizeApi
import com.basistheory.android.view.TextElement
import com.github.javafaker.Faker
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.util.*
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
class BasisTheoryElementsTests {
    private val faker = Faker()
    private lateinit var nameElement: TextElement
    private lateinit var phoneNumberElement: TextElement

    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var tokenizeApi: TokenizeApi

    @RelaxedMockK
    private lateinit var provider: ApiClientProvider

    @Inject
    private val dispatcher = Dispatchers.Unconfined

    @InjectMockKs
    private lateinit var bt: BasisTheoryElements

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()

        nameElement = TextElement(activity)
        phoneNumberElement = TextElement(activity)
    }

    @Test
    fun `tokenize should pass api key override to ApiClientProvider`() = runBlocking {
        val apiKeyOverride = UUID.randomUUID().toString()

        every { provider.getTokenizeApi(any()) } returns tokenizeApi

        bt.tokenize(object {}, apiKeyOverride)

        verify { provider.getTokenizeApi(apiKeyOverride) }
    }

    @Test
    fun `tokenize should forward top level primitive value without modification`() =
        runBlocking {
            every { provider.getTokenizeApi(any()) } returns tokenizeApi

            val name = faker.name().fullName()
            bt.tokenize(name)

            verify { tokenizeApi.tokenize(name) }
        }

    @Test
    fun `tokenize should forward primitive data values within request without modification`() =
        runBlocking {
            every { provider.getTokenizeApi(any()) } returns tokenizeApi

            val request = object {
                val type = "token"
                val data = "primitive"
            }

            bt.tokenize(request)

            val expectedRequest = mapOf<String, Any?>(
                "type" to request.type,
                "data" to request.data
            )

            verify { tokenizeApi.tokenize(expectedRequest) }
        }

    @Test
    fun `tokenize should forward complex data values within request without modification`() =
        runBlocking {
            every { provider.getTokenizeApi(any()) } returns tokenizeApi

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
                }
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
                    )
                )
            )

            verify { tokenizeApi.tokenize(expectedRequest) }
        }

    @Test
    fun `tokenize should replace top level Element ref with underlying data value`() =
        runBlocking {
            every { provider.getTokenizeApi(any()) } returns tokenizeApi

            val name = faker.name().fullName()
            nameElement.setText(name)

            bt.tokenize(nameElement)

            verify { tokenizeApi.tokenize(name) }
        }

    @Test
    fun `tokenize should replace Element refs within request object with underlying data values`() =
        runBlocking {
            every { provider.getTokenizeApi(any()) } returns tokenizeApi

            val name = faker.name().fullName()
            nameElement.setText(name)

            val phoneNumber = faker.phoneNumber().phoneNumber()
            phoneNumberElement.setText(phoneNumber)

            val request = object {
                val type = "token"
                val data = object {
                    val raw = faker.lorem().word()
                    val name = nameElement
                    val nested = object {
                        val raw = faker.lorem().word()
                        val phoneNumber = phoneNumberElement
                    }
                }
            }

            bt.tokenize(request)

            val expectedRequest = mapOf<String, Any?>(
                "type" to request.type,
                "data" to mapOf(
                    "raw" to request.data.raw,
                    "name" to name,
                    "nested" to mapOf(
                        "raw" to request.data.nested.raw,
                        "phoneNumber" to phoneNumber
                    )
                )
            )

            verify { tokenizeApi.tokenize(expectedRequest) }
        }
}