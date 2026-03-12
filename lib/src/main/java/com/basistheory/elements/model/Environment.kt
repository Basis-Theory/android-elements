package com.basistheory.elements.model

enum class Environment {
    DEFAULT,
    US,
    EU,
    TEST
}

internal fun Environment.toJava(): com.basistheory.core.Environment =
    when (this) {
        Environment.DEFAULT -> com.basistheory.core.Environment.DEFAULT
        Environment.US -> com.basistheory.core.Environment.US
        Environment.EU -> com.basistheory.core.Environment.EU
        Environment.TEST -> com.basistheory.core.Environment.TEST
    }
