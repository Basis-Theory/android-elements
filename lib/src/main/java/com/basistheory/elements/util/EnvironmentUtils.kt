package com.basistheory.elements.util

import com.basistheory.elements.model.Environment

const val ApiUrl: String = "https://api.basistheory.com"
const val ApiTestUrl: String = "https://api.test.basistheory.com"
const val ApiUsUrl: String = "https://api.us.basistheory.com"
const val ApiEuUrl: String = "https://api.eu.basistheory.com"

fun Environment.getApiUrl(): String =
    when (this) {
        Environment.US -> ApiUsUrl
        Environment.EU -> ApiEuUrl
        Environment.TEST -> ApiTestUrl
        Environment.DEFAULT -> ApiUrl
    }