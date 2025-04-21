package com.basistheory.elements.model

import java.time.OffsetDateTime

class CreateSessionResponse(
    var sessionKey: String,

    var nonce: String,

    var expiresAt: OffsetDateTime
)

internal fun com.basistheory.types.CreateSessionResponse.toAndroid(): CreateSessionResponse =
    CreateSessionResponse(
        this.sessionKey!!.toString(),
        this.nonce!!.toString(),
        this.expiresAt.get()
    )