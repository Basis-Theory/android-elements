package com.basistheory.elements.model

import com.basistheory.types.CardDetailsResponse
import com.google.gson.annotations.SerializedName

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
        val issuer: CardIssuer? = null
    )
    
    companion object {
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
                        }
                    )
                }
            )
        }
    }
}
