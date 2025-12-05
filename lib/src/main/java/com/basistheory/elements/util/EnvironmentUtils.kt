package com.basistheory.elements.util

import com.basistheory.core.Environment

const val ApiUrl: String = "https://api.basistheory.com"
const val ApiTestUrl: String = "https://api.test.basistheory.com"

fun Environment.getApiUrl(): String =
    when (this) {
        Environment.US -> ApiUrl
        Environment.EU -> ApiUrl
        Environment.TEST -> ApiTestUrl
        Environment.DEFAULT -> ApiUrl
        else -> ApiUrl
    }