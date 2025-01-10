package com.basistheory.elements.view.transform

abstract class ElementTransform internal constructor() {
    abstract fun apply(value: String?): String?
}