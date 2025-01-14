package com.basistheory.elements.view

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import com.basistheory.elements.R
import com.basistheory.elements.constants.ElementValueType
import com.basistheory.elements.event.BlurEvent
import com.basistheory.elements.event.ChangeEvent
import com.basistheory.elements.event.ElementEventListeners
import com.basistheory.elements.event.FocusEvent
import com.basistheory.elements.model.ElementValueReference
import com.basistheory.elements.model.InputAction
import com.basistheory.elements.model.InputType
import com.basistheory.elements.view.mask.ElementMask
import com.basistheory.elements.view.method.FullyHiddenTransformationMethod
import com.basistheory.elements.view.transform.ElementTransform
import com.basistheory.elements.view.validation.ElementValidator

open class TextElement @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val _editText = SensitiveEditText(context)
    private var _defaultBackground = _editText.background
    private val _eventListeners = ElementEventListeners()
    private var _isInternalChange: Boolean = false
    private var _isValid: Boolean = true
    private var _isMaskSatisfied: Boolean = true
    private var _isEmpty: Boolean = true
    private var _inputType: InputType = InputType.TEXT
    private var _getValueType: ElementValueType = ElementValueType.STRING
    private var _enableCopy: Boolean = false
    private var copyIcon = ResourcesCompat.getDrawable(resources, R.drawable.copy, null)
    private var checkIcon = ResourcesCompat.getDrawable(resources, R.drawable.check, null)

    internal var inputAction: InputAction = InputAction.INSERT

    init {
        _editText.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        super.addView(_editText)

        context.theme.obtainStyledAttributes(attrs, R.styleable.TextElement, defStyleAttr, 0)
            .apply {
                try {
                    // map standard android attributes
                    hint = getString(R.styleable.TextElement_android_hint)

                    inputType = InputType.fromAndroidAttr(
                        getInt(
                            R.styleable.TextElement_android_inputType,
                            android.text.InputType.TYPE_CLASS_TEXT
                        )
                    )

                    isEditable = getBoolean(R.styleable.TextElement_android_enabled, true)

                    setText(getString(R.styleable.TextElement_android_text))

                    textColor = getColor(R.styleable.TextElement_android_textColor, Color.BLACK)

                    textSize = getDimension(
                        R.styleable.TextElement_android_textSize,
                        16f * resources.displayMetrics.scaledDensity
                    )

                    hintTextColor =
                        getColor(R.styleable.TextElement_android_textColorHint, Color.LTGRAY)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        textAutofillHints =
                            getString(R.styleable.TextElement_android_autofillHints)?.split(",")
                                ?.toTypedArray()
                    }

                    typeface = resolveTypeface(
                        getInt(R.styleable.TextElement_android_typeface, 0),
                        defStyleAttr
                    )

                    gravity = Gravity.START

                    enableCopy = false

                    // map custom attributes
                    mask = getString(R.styleable.TextElement_bt_mask)?.let { ElementMask(it) }

                    removeDefaultStyles = getBoolean(
                        R.styleable.TextElement_bt_removeDefaultStyles,
                        true
                    )
                } finally {
                    recycle()
                }
            }

        subscribeToInputEvents()
    }

    // the following getters MUST be internal to prevent host apps from accessing the raw input values

    internal fun getText(): String? {
        _editText.allowTextAccess = true
        val text = _editText.text?.toString()
        _editText.allowTextAccess = false

        return text
    }

    internal fun getTransformedText(): String? {
        _editText.allowTextAccess = true
        val transformedText = _editText.text?.toString().let {
            transform?.apply(it) ?: it
        }
        _editText.allowTextAccess = false

        return transformedText
    }

    fun setText(value: String?) =
        _editText.setText(value)

    fun setValueRef(element: TextElement) {
        element.addChangeEventListener {
            setText(element.getText())
            _editText.requestLayout()
        }
    }

    fun setValueRef(elementValueReference: ElementValueReference) {
        setText(elementValueReference.getValue())
        _editText.requestLayout()
    }

    fun setDrawables(startDrawable: Int, topDrawable: Int, endDrawable: Int, bottomDrawable: Int) {
        _editText.setCompoundDrawablesWithIntrinsicBounds(
            startDrawable,
            topDrawable,
            endDrawable,
            bottomDrawable
        )
    }

    fun showKeyboard(flags: Int) {
        val imm = context.getSystemService<InputMethodManager>()
        imm!!.showSoftInput(_editText, flags)
    }

    fun getDrawables(): Array<Drawable?> {
        return _editText.compoundDrawables
    }

    val isComplete: Boolean
        get() = _isMaskSatisfied && _isValid

    val isValid: Boolean
        get() = _isValid

    val isMaskSatisfied: Boolean
        get() = _isMaskSatisfied

    val isEmpty: Boolean
        get() = _isEmpty

    var isEditable: Boolean
        get() = _editText.isEnabled
        set(value) {
            isEnabled = value
            _editText.isEnabled = value
        }

    var mask: ElementMask? = null
        set(value) {
            field = value
            _isMaskSatisfied = mask == null
        }

    var transform: ElementTransform? = null

    var validator: ElementValidator? = null
        set(value) {
            field = value
            _isValid = validator == null
        }

    var textColor: Int
        get() = _editText.currentTextColor
        set(value) = _editText.setTextColor(value)

    var textSize: Float
        get() = _editText.textSize
        set(value) = _editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, value)

    var typeface: Typeface?
        get() = _editText.typeface
        set(value) {
            _editText.typeface = value
        }

    var gravity: Int
        get() = _editText.gravity
        set(value) {
            _editText.gravity = value
        }

    var hint: CharSequence?
        get() = _editText.hint
        set(value) {
            _editText.hint = value
        }

    var hintTextColor: Int
        get() = _editText.currentHintTextColor
        set(value) {
            _editText.setHintTextColor(value)
        }

    var inputType: InputType
        get() = _inputType
        set(value) {
            _inputType = value
            _editText.inputType = value.androidInputType

            if (value.isConcealed)
                _editText.transformationMethod = FullyHiddenTransformationMethod()
        }

    var getValueType: ElementValueType
        get() = _getValueType
        set(value) {
            _getValueType = value
        }

    var removeDefaultStyles: Boolean
        get() = _editText.background == null
        set(value) {
            _editText.background = if (value) null else _defaultBackground
        }

    var enableCopy: Boolean = false
        set(value) {
            field = value
            if (value && !_enableCopy) {
                _enableCopy = true
                setupCopy()
            } else if (_enableCopy && !value) {
                _enableCopy = false
                removeCopy()
            }
        }

    var copyIconColor: Int = Color.BLACK
        set(value) {
            field = value

            copyIcon?.setTint(value)
            checkIcon?.setTint(value)
        }

    var textAutofillHints: Array<String>?
        @RequiresApi(Build.VERSION_CODES.O)
        get() = _editText.autofillHints
        @RequiresApi(Build.VERSION_CODES.O)
        set(value) {
            value?.let { _editText.setAutofillHints(*value) }
        }

    fun addChangeEventListener(listener: (ChangeEvent) -> Unit) {
        _eventListeners.change.add(listener)
    }

    fun addFocusEventListener(listener: (FocusEvent) -> Unit) {
        _eventListeners.focus.add(listener)
    }

    fun addBlurEventListener(listener: (BlurEvent) -> Unit) {
        _eventListeners.blur.add(listener)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        return _editText.onCreateInputConnection(outAttrs)
    }

    override fun onSaveInstanceState(): Parcelable {
        return bundleOf(
            STATE_SUPER to super.onSaveInstanceState(),
            STATE_INPUT to _editText.onSaveInstanceState()
        )
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            _editText.onRestoreInstanceState(state.getParcelable(STATE_INPUT))
            super.onRestoreInstanceState(state.getParcelable(STATE_SUPER))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    protected open fun beforeTextChanged(value: String?): String? = value

    protected open fun createElementChangeEvent(): ChangeEvent =
        ChangeEvent(
            isComplete,
            isEmpty,
            isValid,
            isMaskSatisfied,
            mutableListOf()
        )

    private fun subscribeToInputEvents() {
        _editText.allowTextAccess = true
        _editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                value: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                value: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (_isInternalChange) return

                inputAction =
                    if (before > 0 && count == 0) InputAction.DELETE
                    else InputAction.INSERT
            }

            override fun afterTextChanged(editable: Editable?) {
                afterTextChangedHandler(editable)
            }
        })
        _editText.allowTextAccess = false

        _editText.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                _eventListeners.focus.iterator().forEach { it(FocusEvent()) }
            else
                _eventListeners.blur.iterator().forEach { it(BlurEvent()) }
        }
    }

    private fun afterTextChangedHandler(editable: Editable?) {
        if (_isInternalChange) return

        val originalValue = editable?.toString()
        val transformedValue = beforeTextChanged(originalValue)
            .let { mask?.evaluate(it, inputAction) ?: it }

        if (originalValue != transformedValue)
            applyInternalChange(transformedValue)

        _isValid = validator?.validate(getTransformedText()) ?: true
        _isMaskSatisfied = mask?.isSatisfied(editable?.toString()) ?: true
        _isEmpty = editable?.toString()?.isEmpty() ?: true

        publishChangeEvent()
    }

    private fun applyInternalChange(value: String?) {
        val editable = _editText.editableText
        val originalFilters = editable.filters

        _isInternalChange = true

        // disable filters on the underlying input applied by the input/keyboard type
        editable.filters = emptyArray()
        editable.replace(0, editable.length, value)
        editable.filters = originalFilters

        _isInternalChange = false
    }

    protected fun publishChangeEvent() {
        val event = createElementChangeEvent()

        _eventListeners.change.iterator().forEach {
            it(event)
        }
    }

    private fun resolveTypeface(typefaceIndex: Int, style: Int): Typeface? =
        when (typefaceIndex) {
            1 -> Typeface.create(Typeface.SANS_SERIF, style)
            2 -> Typeface.create(Typeface.SERIF, style)
            3 -> Typeface.create(Typeface.MONOSPACE, style)
            else -> Typeface.defaultFromStyle(style)
        }

    internal companion object {
        private const val STATE_SUPER = "state_super"
        private const val STATE_INPUT = "state_input"
    }

    @SuppressLint("ClickableViewAccessibility") // accessibility handled by adding both touch and click listeners
    private fun setupCopy() {
        _editText.setCompoundDrawablesWithIntrinsicBounds(null, null, copyIcon, null)

        val drawableRight = 2
        val icon = _editText.compoundDrawables[drawableRight]
        val iconTouchArea = icon.bounds.width()

        copyIcon?.setTint(copyIconColor)

        _editText.setOnClickListener {
            val clickX = it.x.toInt()

            if (clickX >= _editText.right - iconTouchArea) {
                copyTextToClipboard()
            }
        }

        _editText.setOnTouchListener(OnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {

                if (event.rawX >= _editText.right - iconTouchArea) {
                    copyTextToClipboard()
                    return@OnTouchListener true
                }
            }
            false
        })
    }

    private fun copyTextToClipboard() {
        // copy to clipboard
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("Value", getText())
        clipboard.setPrimaryClip(clip)

        // update icon
        _editText.setCompoundDrawablesWithIntrinsicBounds(null, null, checkIcon, null)

        // update back
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            _editText.setCompoundDrawablesWithIntrinsicBounds(null, null, copyIcon, null)
        }
        handler.postDelayed(runnable, 1000)
    }

    private fun removeCopy() {
        _editText.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
    }
}
