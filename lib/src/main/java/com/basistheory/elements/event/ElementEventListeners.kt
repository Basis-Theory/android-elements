package com.basistheory.elements.event

class ElementEventListeners {
    val change: MutableList<(ChangeEvent) -> Unit> = mutableListOf()
    val focus: MutableList<(FocusEvent) -> Unit> = mutableListOf()
    val blur: MutableList<(BlurEvent) -> Unit> = mutableListOf()
}
