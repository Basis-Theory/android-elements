package com.basistheory.elements.constants

enum class CoBadgedSupport(val value: String) {
    CARTES_BANCAIRES("cartes-bancaires");

    companion object {
        fun fromString(value: String): CoBadgedSupport? {
            return values().find { it.value.equals(value, ignoreCase = true) }
        }

        fun validate(values: List<String>): Boolean {
            val validValues = values().map { it.value }
            return values.all { validValues.contains(it.lowercase()) }
        }
    }
}
