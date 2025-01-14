package com.basistheory.elements.example.util

import com.google.gson.GsonBuilder

fun Any.prettyPrintJson(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(this)
}
