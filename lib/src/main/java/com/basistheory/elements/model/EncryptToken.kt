package com.basistheory.elements.model

class EncryptTokenRequest(
    var tokenRequests: Any,

    var publicKey: String,

    var keyId: String
)

class EncryptTokenResponse(
    var encrypted: String,
    var type: String
)