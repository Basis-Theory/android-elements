package com.basistheory.elements.model

import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class Token(
    var id: String,

    var tenantId: UUID,

    var type: String,

    var data: Any?,

    var mask: Any? = null,

    var fingerprint: String? = null,

    var fingerprintExpression: String? = null,

    var enrichments: Any? = null,

    var expiresAt: OffsetDateTime? = null,

    var createdBy: UUID,

    var createdAt: OffsetDateTime,

    var modifiedBy: UUID? = null,

    var modifiedAt: OffsetDateTime? = null,

    var metadata: Map<String, String>? = null,

    var containers: List<String>,

    var searchIndexes: List<String>? = null,
)

internal fun com.basistheory.types.Token.toAndroid(): Token = Token(
        this.id.toString(),
        this.tenantId.toUUIDOrThrow(),
        this.type.toString(),
        this.data,
        this.mask,
        this.fingerprint.orElse(null),
        this.fingerprintExpression.orElse(null),
        this.enrichments,
        this.expiresAt.orElse(null),
        this.createdBy.toUUIDOrThrow(),
        this.createdAt.orElse(null),
        this.modifiedBy.toUUIDOrNull(),
        this.modifiedAt.orElse(null),
        this.metadata.orElse(null)?.run {
            mapNotNull { (k, v) -> v.orElse(null)?.let { k to it } }.toMap()
        },
        this.containers.orElse(emptyList()),
        this.searchIndexes.orElse(emptyList())
    )


internal fun Optional<String>.toUUIDOrNull(): UUID? =
    this
        .map(UUID::fromString)
        .orElse(null)

internal fun Optional<String>.toUUIDOrThrow() =
    this
        .map(UUID::fromString)
        .orElseThrow()