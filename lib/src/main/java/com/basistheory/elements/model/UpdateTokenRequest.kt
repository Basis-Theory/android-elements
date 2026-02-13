package com.basistheory.elements.model

import java.util.Optional

class UpdateTokenRequest(
    var data: Any,

    var mask: Any? = null,

    var fingerprintExpression: String? = null,

    var deduplicateToken: Boolean? = null,

    var expiresAt: String? = null,

    var metadata: Map<String, Optional<String>>? = null,

    var containers: List<String>? = null,

    var searchIndexes: List<String>? = null,
)

internal fun UpdateTokenRequest.toJava(): com.basistheory.resources.tokens.requests.UpdateTokenRequest =
    com.basistheory.resources.tokens.requests.UpdateTokenRequest.builder()
        .data(this@toJava.data)
        .mask(this@toJava.mask)
        .fingerprintExpression(this@toJava.fingerprintExpression)
        .deduplicateToken(this@toJava.deduplicateToken)
        .expiresAt(this@toJava.expiresAt)
        .metadata(this@toJava.metadata)
        .containers(this@toJava.containers)
        .searchIndexes(this@toJava.searchIndexes)
        .build()
