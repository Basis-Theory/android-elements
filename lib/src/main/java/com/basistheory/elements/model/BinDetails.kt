package com.basistheory.elements.model

import com.basistheory.types.CardDetailsResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * Represents a BIN range with minimum and maximum values
 */
data class BinRange(
    @SerializedName("binMin")
    val binMin: String,

    @SerializedName("binMax")
    val binMax: String
)

/**
 * Represents detailed information about a card BIN (Bank Identification Number)
 * Retrieved from the Basis Theory enrichments API when a card number reaches 6+ digits
 */
data class BinDetails(
    @SerializedName("brand")
    val brand: String? = null,
    
    @SerializedName("funding")
    val funding: String? = null,
    
    @SerializedName("segment")
    val segment: String? = null,
    
    @SerializedName("issuer")
    val issuer: CardIssuer? = null,
    
    @SerializedName("binRange")
    val binRange: List<BinRange>? = null,

    @SerializedName("additional")
    val additional: List<AdditionalCardDetail>? = null
) {
    data class CardIssuer(
        @SerializedName("name")
        val name: String? = null,
        
        @SerializedName("country")
        val country: String? = null
    )

    data class AdditionalCardDetail(
        @SerializedName("brand")
        val brand: String? = null,

        @SerializedName("funding")
        val funding: String? = null,

        @SerializedName("segment")
        val segment: String? = null,

        @SerializedName("issuer")
        val issuer: CardIssuer? = null,

        @SerializedName("binRange")
        val binRange: List<BinRange>? = null
    )
    
    companion object {
        private val gson = Gson()

        private fun parseBinRanges(jsonObject: JsonObject?): List<BinRange>? {
            return try {
                jsonObject?.getAsJsonArray("binRange")?.map { element ->
                    val rangeObj = element.asJsonObject
                    BinRange(
                        binMin = rangeObj.get("binMin")?.asString ?: "",
                        binMax = rangeObj.get("binMax")?.asString ?: ""
                    )
                }
            } catch (e: Exception) {
                null
            }
        }

        fun fromResponse(response: CardDetailsResponse): BinDetails {
            return BinDetails(
                brand = response.brand.orElse(null),
                funding = response.funding.orElse(null),
                segment = response.segment.orElse(null),
                issuer = response.issuer.orElse(null)?.let { issuer ->
                    CardIssuer(
                        name = issuer.name.orElse(null),
                        country = issuer.country.orElse(null)
                    )
                },
                binRange = response.binRange.orElse(null)?.map { range ->
                    BinRange(
                        binMin = range.binMin.orElse(""),
                        binMax = range.binMax.orElse("")
                    )
                },
                additional = response.additional.orElse(null)?.map { additional ->
                    AdditionalCardDetail(
                        brand = additional.brand.orElse(null),
                        funding = additional.funding.orElse(null),
                        segment = additional.segment.orElse(null),
                        issuer = additional.issuer.orElse(null)?.let { issuer ->
                            CardIssuer(
                                name = issuer.name.orElse(null),
                                country = issuer.country.orElse(null)
                            )
                        },
                        binRange = additional.binRange.orElse(null)?.map { range ->
                            BinRange(
                                binMin = range.binMin.orElse(""),
                                binMax = range.binMax.orElse("")
                            )
                        }
                    )
                }
            )
        }
    }
}
