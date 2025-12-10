package com.basistheory.elements.example

import android.content.Intent
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.basistheory.elements.constants.CoBadgedSupport
import com.basistheory.elements.view.CardBrandSelector
import com.basistheory.elements.view.CardBrandSelectorOptions
import com.basistheory.elements.view.CardNumberElement
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardBrandSelectorTests {

    private lateinit var cardBrandSelector: CardBrandSelector
    private lateinit var cardNumberElement: CardNumberElement

    @Before
    fun setUp() {
        cardNumberElement = CardNumberElement(ApplicationProvider.getApplicationContext())
        cardNumberElement.binLookup = true
        cardNumberElement.coBadgedSupport = listOf(CoBadgedSupport.CARTES_BANCAIRES)

        cardBrandSelector = CardBrandSelector(ApplicationProvider.getApplicationContext())

        val options = CardBrandSelectorOptions(cardNumberElement = cardNumberElement)
        cardBrandSelector.setConfig(options)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun testInitialState() {
        assertNull(cardBrandSelector.getSelectedCardBrand())
        assertEquals(0, cardBrandSelector.getAvailableCardBrands().size)
        assertEquals(View.GONE, cardBrandSelector.visibility)
    }

    @Test
    fun testBrandSelectorHiddenWhenNoCoBadgedSupport() {
        val textFieldWithoutCobadge = CardNumberElement(ApplicationProvider.getApplicationContext())
        textFieldWithoutCobadge.binLookup = true
        textFieldWithoutCobadge.coBadgedSupport = emptyList()

        val options = CardBrandSelectorOptions(cardNumberElement = textFieldWithoutCobadge)
        cardBrandSelector.setConfig(options)

        val intent = Intent(CardBrandSelector.ACTION_BRAND_OPTIONS_UPDATED).apply {
            putStringArrayListExtra(CardBrandSelector.EXTRA_BRAND_OPTIONS, arrayListOf("visa"))
        }
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()).sendBroadcast(intent)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(View.GONE, cardBrandSelector.visibility)
        assertEquals(1, cardBrandSelector.getAvailableCardBrands().size)
    }

    @Test
    fun testBrandSelectorVisibleWithMultipleSupportedBrands() {
        val intent = Intent(CardBrandSelector.ACTION_BRAND_OPTIONS_UPDATED).apply {
            putStringArrayListExtra(CardBrandSelector.EXTRA_BRAND_OPTIONS, arrayListOf("visa", "cartes-bancaires"))
        }
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()).sendBroadcast(intent)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(View.VISIBLE, cardBrandSelector.visibility)
        assertEquals(2, cardBrandSelector.getAvailableCardBrands().size)
        assertTrue(cardBrandSelector.getAvailableCardBrands().contains("visa"))
        assertTrue(cardBrandSelector.getAvailableCardBrands().contains("cartes-bancaires"))
    }

    @Test
    fun testBrandSelectorHiddenWithSingleBrand() {
        val intent = Intent(CardBrandSelector.ACTION_BRAND_OPTIONS_UPDATED).apply {
            putStringArrayListExtra(CardBrandSelector.EXTRA_BRAND_OPTIONS, arrayListOf("visa"))
        }
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()).sendBroadcast(intent)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(View.GONE, cardBrandSelector.visibility)
        assertEquals(1, cardBrandSelector.getAvailableCardBrands().size)
    }

    @Test
    fun testBrandSelectionUpdatesSelectedBrand() {
        val intent = Intent(CardBrandSelector.ACTION_BRAND_OPTIONS_UPDATED).apply {
            putStringArrayListExtra(CardBrandSelector.EXTRA_BRAND_OPTIONS, arrayListOf("visa", "cartes-bancaires"))
        }
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()).sendBroadcast(intent)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(2, cardBrandSelector.getAvailableCardBrands().size)
        assertTrue(cardBrandSelector.getAvailableCardBrands().contains("visa"))
        assertTrue(cardBrandSelector.getAvailableCardBrands().contains("cartes-bancaires"))

        cardBrandSelector.setSelectedBrand("cartes-bancaires")

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals("cartes-bancaires", cardBrandSelector.getSelectedCardBrand())
    }

    @Test
    fun testBrandSelectionSendsBroadcast() {
        val intent = Intent(CardBrandSelector.ACTION_BRAND_OPTIONS_UPDATED).apply {
            putStringArrayListExtra(CardBrandSelector.EXTRA_BRAND_OPTIONS, arrayListOf("visa", "cartes-bancaires"))
        }
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()).sendBroadcast(intent)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        var receivedBrand: String? = null
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                receivedBrand = intent?.getStringExtra(CardBrandSelector.EXTRA_SELECTED_BRAND)
            }
        }

        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
            .registerReceiver(receiver, android.content.IntentFilter(CardBrandSelector.ACTION_BRAND_SELECTED))

        cardBrandSelector.setSelectedBrand("cartes-bancaires")

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals("cartes-bancaires", receivedBrand)

        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()).unregisterReceiver(receiver)
    }

    @Test
    fun testBrandSelectionTriggersCallback() {
        val intent = Intent(CardBrandSelector.ACTION_BRAND_OPTIONS_UPDATED).apply {
            putStringArrayListExtra(CardBrandSelector.EXTRA_BRAND_OPTIONS, arrayListOf("visa", "cartes-bancaires"))
        }
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()).sendBroadcast(intent)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        var callbackBrand: String? = null
        cardBrandSelector.onBrandSelection { selectedBrand ->
            callbackBrand = selectedBrand
        }

        cardBrandSelector.setSelectedBrand("cartes-bancaires")

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals("cartes-bancaires", callbackBrand)
    }

    @Test
    fun testBrandSelectorClearedWhenBinInfoCleared() {
        val intent1 = Intent(CardBrandSelector.ACTION_BRAND_OPTIONS_UPDATED).apply {
            putStringArrayListExtra(CardBrandSelector.EXTRA_BRAND_OPTIONS, arrayListOf("visa", "cartes-bancaires"))
        }
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()).sendBroadcast(intent1)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(2, cardBrandSelector.getAvailableCardBrands().size)

        val intent2 = Intent(CardBrandSelector.ACTION_BRAND_OPTIONS_UPDATED).apply {
            putStringArrayListExtra(CardBrandSelector.EXTRA_BRAND_OPTIONS, arrayListOf())
        }
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext()).sendBroadcast(intent2)

        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        assertEquals(0, cardBrandSelector.getAvailableCardBrands().size)
        assertNull(cardBrandSelector.getSelectedCardBrand())
    }
}
