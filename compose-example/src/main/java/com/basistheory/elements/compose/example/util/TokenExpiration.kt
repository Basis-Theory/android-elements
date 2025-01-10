package com.basistheory.elements.compose.example.util

import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit

fun tokenExpirationTimestamp() = Instant.now()
    .plus(5, ChronoUnit.MINUTES)
    .toString()