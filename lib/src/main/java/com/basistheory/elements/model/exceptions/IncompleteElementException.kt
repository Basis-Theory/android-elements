package com.basistheory.elements.model.exceptions

class IncompleteElementException(id: Int) : Exception(errorMessageFor(id)) {
    companion object {
        fun errorMessageFor(id: Int) = "Element id=$id is in an incomplete state"
    }
}