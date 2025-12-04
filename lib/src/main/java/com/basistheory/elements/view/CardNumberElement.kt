package com.basistheory.elements.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import com.basistheory.elements.event.ChangeEvent
import com.basistheory.elements.event.EventDetails
import com.basistheory.elements.model.BinDetails
import com.basistheory.elements.model.CardMetadata
import com.basistheory.elements.model.InputType
import com.basistheory.elements.service.CardBrandEnricher
import com.basistheory.elements.util.BinDetailsCache
import com.basistheory.elements.view.mask.ElementMask
import com.basistheory.elements.view.transform.RegexReplaceElementTransform
import com.basistheory.elements.view.validation.LuhnValidator
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

    init {
        super.inputType = InputType.NUMBER
        super.mask = defaultMask
        super.transform = RegexReplaceElementTransform(Regex("""\s"""), "")
        super.validator = LuhnValidator()
    }

    var cardMetadata: CardMetadata? = null
        private set

    internal var cvcMask: String? = null

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

        if (binLookup && bin != null && bin.length == 6 && bin != lastFetchedBin) {
            fetchBinDetails(bin)
        }

        if (bin == null || bin.length < 6) {
            lastFetchedBin = null
            if (binDetails != null) {
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
        this.binDetails = binDetails
        publishChangeEvent()
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

        this.binDetails?.let {
            eventDetails.add(
                EventDetails(
                    EventDetails.BinDetails,
                    "binDetails",
                    it
                )
            )
        }

        return ChangeEvent(
            isComplete,
            isEmpty,
            isValid,
            isMaskSatisfied,
            eventDetails
        )
    }

    private fun getDigitsOnly(text: String?): String? {
        val maskedValue = mask?.evaluate(text, inputAction)
        return transform?.apply(maskedValue) ?: maskedValue
    }

    private fun String.binLength() = if(this.length >= 16) 8 else 6

    companion object {
        private val digit = Regex("""\d""")

        val defaultMask = ElementMask(
            (1..19).map {
                if (it % 5 == 0 && it > 0) " " else digit
            }
        )
    }
}
