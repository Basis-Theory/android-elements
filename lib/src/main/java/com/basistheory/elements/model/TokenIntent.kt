package com.basistheory.elements.model

import com.google.gson.annotations.SerializedName
import java.time.OffsetDateTime
import java.util.UUID

class TokenIntent(
    val id: String,
    val tenantId: UUID,
    val type: String,
    val card: Any? = null,
    val bank: Any? = null,
    val networkToken: Any? = null,
    val authentication: Any? = null,
    val fingerprint: String? = null,
    val expiresAt: OffsetDateTime,
    val createdBy: UUID,
    val createdAt: OffsetDateTime,
    val extras: Any? = null
)

internal fun com.basistheory.types.CreateTokenIntentResponse.toAndroid(): TokenIntent = TokenIntent(
    id = this.id.toString(),
    tenantId = this.tenantId.orElse(null)?.let { UUID.fromString(it) } ?: throw IllegalStateException("TokenIntent tenantId is required"),
    type = this.type.toString(),
    card = this.card.orElse(null),
    bank = this.bank.orElse(null),
    networkToken = this.networkToken.orElse(null),
    authentication = this.authentication.orElse(null),
    fingerprint = this.fingerprint.orElse(null),
    expiresAt = this.expiresAt.orElse(null) ?: throw IllegalStateException("TokenIntent expiresAt is required"),
    createdBy = this.createdBy.orElse(null)?.let { UUID.fromString(it) } ?: throw IllegalStateException("TokenIntent createdBy is required"),
    createdAt = this.createdAt.orElse(null) ?: throw IllegalStateException("TokenIntent createdAt is required"),
    extras = this.extras.orElse(null)
)

