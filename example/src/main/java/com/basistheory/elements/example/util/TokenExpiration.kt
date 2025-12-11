package com.basistheory.elements.example.util

import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit

fun tokenExpirationTimestamp() = Instant.now()
    .plus(30, ChronoUnit.MINUTES)
    .toString()