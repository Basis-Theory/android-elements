package com.basistheory.elements.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.AttributeSet
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.basistheory.elements.constants.CardBrands
import com.basistheory.elements.constants.CoBadgedSupport
import com.basistheory.elements.event.ChangeEvent
import com.basistheory.elements.event.EventDetails
import com.basistheory.elements.model.BinDetails
import com.basistheory.elements.model.BinRange
import com.basistheory.elements.model.CardMetadata
import com.basistheory.elements.model.InputType
import com.basistheory.elements.service.ApiClientProvider
import com.basistheory.elements.service.CardBrandEnricher
import com.basistheory.elements.util.BinDetailsCache
import com.basistheory.elements.view.mask.ElementMask
import com.basistheory.elements.view.transform.RegexReplaceElementTransform
import com.basistheory.elements.view.validation.LuhnValidator
import com.basistheory.resources.enrichments.requests.EnrichmentsGetCardDetailsRequest
import com.basistheory.types.CardDetailsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CardNumberElement @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextElement(context, attrs, defStyleAttr) {

    private val cardBrandEnricher: CardBrandEnricher = CardBrandEnricher()
    private var binDetailsFetchJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var lastFetchedBin: String? = null
    private var basisTheoryElements: com.basistheory.elements.service.BasisTheoryElements? = null

    var coBadgedSupport: List<CoBadgedSupport>? = null
    internal var selectedNetwork: String? = null
    private var lastBrandOptions: List<String> = emptyList()

    private val brandSelectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == CardBrandSelector.ACTION_BRAND_SELECTED) {
                val brandName = intent.getStringExtra(CardBrandSelector.EXTRA_SELECTED_BRAND)
                brandName?.let {
                    selectedNetwork = it
                    publishChangeEvent()
                }
            }
        }
    }

    init {
        super.inputType = InputType.NUMBER
        super.mask = defaultMask
        super.transform = RegexReplaceElementTransform(Regex("""\s"""), "")
        super.validator = LuhnValidator()

        val filter = IntentFilter(CardBrandSelector.ACTION_BRAND_SELECTED)
        LocalBroadcastManager.getInstance(context).registerReceiver(brandSelectionReceiver, filter)
    }

    var cardMetadata: CardMetadata? = null
        private set

    internal var cvcMask: String? = null

    private var rawBinDetails: BinDetails? = null

    var binDetails: BinDetails? = null
        private set

    var binLookup: Boolean = false

    fun setBasisTheoryElements(basisTheoryElements: com.basistheory.elements.service.BasisTheoryElements) {
        this.basisTheoryElements = basisTheoryElements
    }

    override fun beforeTextChanged(value: String?): String? {
        val cardDigits = getDigitsOnly(value)
        val bin = cardDigits?.take(6)
        val cardBrandDetails = cardBrandEnricher.evaluateCard(cardDigits)

        if (cardBrandDetails != null)
            mask = ElementMask(
                cardBrandDetails.cardMask,
                cardBrandDetails.validDigitCounts
                    .map { it + cardBrandDetails.gapCount }
                    .toIntArray()
            )

        val isMaskSatisfied = mask?.isSatisfied(value) ?: true

        cardMetadata = CardMetadata(
            cardBrandDetails?.brand,
            cardDigits?.take(cardDigits.binLength()).takeIf { isMaskSatisfied },
            cardDigits?.takeLast(4).takeIf { isMaskSatisfied },
        )
        cvcMask = cardBrandDetails?.cvcMask

        if (bin != null && bin.length == 6 && (binLookup || !coBadgedSupport.isNullOrEmpty())) {
            if (bin != lastFetchedBin) {
                fetchBinDetails(bin)
            } else if (!coBadgedSupport.isNullOrEmpty()) {
                updateBrandSelectorOptions()
            }
        }

        if (bin == null || bin.length < 6) {
            lastFetchedBin = null
            if (binDetails != null) {
                clearBinInfo()
                binDetails = null
                publishChangeEvent()
            }
        }

        return value
    }

    private fun fetchBinDetails(bin: String) {
        binDetailsFetchJob?.cancel()
        binDetailsFetchJob = coroutineScope.launch {
            try {
                BinDetailsCache.get(bin)?.let { cachedDetails ->
                    updateBinDetails(cachedDetails)
                    return@launch
                }

                val response = basisTheoryElements?.getCardDetails(bin)

                response?.let {
                    val binDetails = BinDetails.fromResponse(it)

                    BinDetailsCache.put(bin, binDetails)
                    updateBinDetails(binDetails)
                }
            } catch (e: Exception) {
                Log.e("CardNumberElement", "Error fetching BIN details", e)
            }
        }
    }

    private fun updateBinDetails(binDetails: BinDetails) {
        this.rawBinDetails = binDetails

        if (!coBadgedSupport.isNullOrEmpty()) {
            updateBrandSelectorOptions()
        }

        publishChangeEvent()
    }

    private fun updateBrandSelectorOptions() {
        val currentBrandOptions = getBrandOptions()

        if (currentBrandOptions != lastBrandOptions) {
            lastBrandOptions = currentBrandOptions
            val intent = Intent(CardBrandSelector.ACTION_BRAND_OPTIONS_UPDATED).apply {
                putStringArrayListExtra(CardBrandSelector.EXTRA_BRAND_OPTIONS, ArrayList(currentBrandOptions))
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    internal fun getBrandOptions(): List<String> {
        val binInfo = getFilteredBinDetails() ?: return emptyList()
        val brands = mutableListOf<String>()

        binInfo.brand?.let { brands.add(normalizeBrandName(it)) }

        val coBadgedSupport = this.coBadgedSupport
        if (coBadgedSupport.isNullOrEmpty()) {
            return brands
        }

        val supportedValues = coBadgedSupport.map { it.value }

        binInfo.additional?.forEach { additional ->
            val rawBrandName = additional.brand ?: return@forEach
            val brandName = normalizeBrandName(rawBrandName)

            if (isValidBrand(brandName, supportedValues)) {
                brands.add(brandName)
            }
        }

        return brands
    }

    private fun normalizeBrandName(brandName: String): String {
        return brandName.lowercase().replace("_", "-")
    }

    private fun isValidBrand(brandName: String, supportedBy: List<String>): Boolean {
        val isValid = CardBrands.values().any { it.label == brandName }
        val isSupported = supportedBy.contains(brandName)

        return isValid && isSupported
    }

    private fun getFilteredBinDetails(): BinDetails? {
        val rawDetails = rawBinDetails ?: return null
        val cardValue = getTransformedText() ?: return null
        val primaryRanges = rawDetails.binRange ?: emptyList()

        val isValidPrimaryRange = primaryRanges.any { range ->
            isCardInBinRange(cardValue, range)
        }

        val filteredAdditionals = rawDetails.additional?.filter { additional ->
            val ranges = additional.binRange ?: return@filter false
            ranges.any { range -> isCardInBinRange(cardValue, range) }
        }

        if (!isValidPrimaryRange && filteredAdditionals.isNullOrEmpty()) {
            return null
        }

        return BinDetails(
            brand = if (isValidPrimaryRange) rawDetails.brand else null,
            funding = if (isValidPrimaryRange) rawDetails.funding else null,
            issuer = if (isValidPrimaryRange) rawDetails.issuer else null,
            segment = if (isValidPrimaryRange) rawDetails.segment else null,
            binRange = if (isValidPrimaryRange) rawDetails.binRange else null,
            additional = filteredAdditionals?.map { additional ->
                BinDetails.AdditionalCardDetail(
                    brand = additional.brand,
                    funding = additional.funding,
                    issuer = additional.issuer,
                    segment = additional.segment,
                    binRange = additional.binRange
                )
            }
        )
    }

    private fun isCardInBinRange(cardValue: String, range: BinRange): Boolean {
        val binLength = minOf(range.binMin.length, cardValue.length)
        val cardBin = cardValue.take(binLength).toLongOrNull() ?: return false
        val binMin = range.binMin.take(binLength).toLongOrNull() ?: return false
        val binMax = range.binMax.take(binLength).toLongOrNull() ?: return false
        return cardBin in binMin..binMax
    }

    private fun clearBinInfo() {
        if (rawBinDetails == null && binDetails == null && selectedNetwork == null) return

        rawBinDetails = null
        binDetails = null
        selectedNetwork = null
        lastFetchedBin = null

        if (!coBadgedSupport.isNullOrEmpty() && lastBrandOptions.isNotEmpty()) {
            lastBrandOptions = emptyList()
            val intent = Intent(CardBrandSelector.ACTION_BRAND_OPTIONS_UPDATED).apply {
                putStringArrayListExtra(CardBrandSelector.EXTRA_BRAND_OPTIONS, ArrayList())
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }

    override fun createElementChangeEvent(): ChangeEvent {
        val eventDetails = mutableListOf<EventDetails>()
        val value = getTransformedText()

        this.cardMetadata?.brand?.let {
            eventDetails.add(
                EventDetails(
                    EventDetails.CardBrand,
                    it
                )
            )
        }

        if (value != null && isMaskSatisfied) {
            eventDetails.add(
                EventDetails(
                    EventDetails.CardBin,
                    value.take(value.binLength())
                )
            )

            eventDetails.add(
                EventDetails(
                    EventDetails.CardLast4,
                    value.takeLast(4)
                )
            )
        }

        val filteredBinDetails = getFilteredBinDetails()
        this.binDetails = filteredBinDetails

        filteredBinDetails?.let {
            eventDetails.add(
                EventDetails(
                    EventDetails.BinDetails,
                    "binDetails",
                    it
                )
            )
        }

        val hasValidAdditionalBrands = getBrandOptions().size > 1
        val needsBrandSelection = !coBadgedSupport.isNullOrEmpty() && hasValidAdditionalBrands && selectedNetwork == null
        val complete = isMaskSatisfied && isValid && !needsBrandSelection

        return ChangeEvent(
            complete,
            isEmpty,
            isValid,
            isMaskSatisfied,
            eventDetails,
            if (hasValidAdditionalBrands) selectedNetwork else null
        )
    }

    private fun getDigitsOnly(text: String?): String? {
        val maskedValue = mask?.evaluate(text, inputAction)
        return transform?.apply(maskedValue) ?: maskedValue
    }

    private fun String.binLength() = if(this.length >= 16) 8 else 6

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(brandSelectionReceiver)
    }

    companion object {
        private val digit = Regex("""\d""")

        val defaultMask = ElementMask(
            (1..19).map {
                if (it % 5 == 0 && it > 0) " " else digit
            }
        )
    }
}
