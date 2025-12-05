package com.basistheory.elements.event

import com.basistheory.elements.model.BinDetails

data class ChangeEvent(
    val isComplete: Boolean,
    val isEmpty: Boolean,
    val isValid: Boolean,
    val isMaskSatisfied: Boolean,
    val details: List<EventDetails> = mutableListOf(),
    val selectedNetwork: String? = null
)

data class EventDetails(
    val type: String,
    val message: String,
    val data: Any? = null
) {
    companion object {
        const val CardBrand = "cardBrand"
        const val CardBin = "cardBin"
        const val CardLast4 = "cardLast4"
        const val BinDetails = "binDetails"
    }
}