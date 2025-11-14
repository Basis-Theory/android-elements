package com.basistheory.elements.model

import java.util.Optional


class CreateTokenRequest(
    var id: String? = null,

    var type: String,

    var data: Any,

    var mask: Any? = null,

    var fingerprintExpression: String? = null,

    var deduplicateToken: Boolean? = null,

    var expiresAt: String? = null,

    var metadata: Map<String, Optional<String>>? = null,

    var containers: List<String>? = null,

    var searchIndexes: List<String>? = null,

    var tokenIntentId: String? = null,
)

internal fun CreateTokenRequest.toJava(): com.basistheory.types.CreateTokenRequest =
    com.basistheory.types.CreateTokenRequest.builder()
        .id(this@toJava.id)
        .data(this@toJava.data)
        .type(this@toJava.type)
        .mask(this@toJava.mask)
        .fingerprintExpression(this@toJava.fingerprintExpression)
        .deduplicateToken(this@toJava.deduplicateToken)
        .expiresAt(this@toJava.expiresAt)
        .metadata(this@toJava.metadata)
        .containers(this@toJava.containers)
        .searchIndexes(this@toJava.searchIndexes)
        .tokenIntentId(this@toJava.tokenIntentId)
        .build()
