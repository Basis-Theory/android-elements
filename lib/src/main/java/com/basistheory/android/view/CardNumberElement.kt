package com.basistheory.android.view

import android.content.Context
import android.util.AttributeSet
import com.basistheory.android.model.KeyboardType
import com.basistheory.android.view.transform.regexReplaceElementTransform
import com.basistheory.android.view.validation.luhnValidator

class CardNumberElement : TextElement {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
      init()
    }

    private fun init() {
        super.keyboardType = KeyboardType.NUMBER
        super.mask = defaultMask
        super.transform = regexReplaceElementTransform(Regex("""\s"""), "")
        super.validate = ::luhnValidator
    }

    companion object {
        private val digit = Regex("""\d""")

        val defaultMask: List<Any> =
            (1..19).map {
                if (it % 5 == 0 && it > 0) " " else digit
            }
    }
}