package com.basistheory.elements.model

class CreateTokenIntentRequest(
    var type: String,
    var data: Any
)

internal fun CreateTokenIntentRequest.toJava(): com.basistheory.resources.tokenintents.requests.CreateTokenIntentRequest =
    com.basistheory.resources.tokenintents.requests.CreateTokenIntentRequest.builder()
        .type(this.type)
        .data(this.data)
        .build()
