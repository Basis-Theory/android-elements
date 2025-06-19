package com.basistheory.elements.model.exceptions

class EncryptTokenException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)