package com.basistheory.elements.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.AttributeSet
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.widget.AppCompatButton
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Configuration options for CardBrandSelector
 */
data class CardBrandSelectorOptions(
    val cardNumberElement: CardNumberElement? = null
)

/**
 * CardBrandSelector is a UI component that allows users to select between multiple card brands
 * when a card number supports co-badging (e.g., VISA and Cartes Bancaires).
 * 
 * This component listens for brand options updates from the CardNumberElement and displays
 * a dropdown menu when multiple brands are available.
 */
class CardBrandSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var cardNumberElement: CardNumberElement? = null
    private var availableBrands: List<String> = emptyList()
    private var selectedBrand: String? = null
    private var brandSelectionCallback: ((String) -> Unit)? = null
    private var defaultTitle: String? = null

    private val brandOptionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_BRAND_OPTIONS_UPDATED -> {
                    val brands = intent.getStringArrayListExtra(EXTRA_BRAND_OPTIONS) ?: emptyList()
                    updateAvailableBrands(brands)
                }
            }
        }
    }

    init {
        defaultTitle = text?.toString() ?: "Select Card Brand"
        setPadding(24, 24, 24, 24)
        
        if (!isInEditMode) {
            visibility = View.GONE
            alpha = 0f
        }
        
        val filter = IntentFilter().apply {
            addAction(ACTION_BRAND_OPTIONS_UPDATED)
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(brandOptionsReceiver, filter)
        
        setOnClickListener {
            showBrandSelectionMenu()
        }
    }

    /**
     * Configure the CardBrandSelector with options
     */
    fun setConfig(options: CardBrandSelectorOptions) {
        cardNumberElement = options.cardNumberElement
    }

    /**
     * Get the currently selected brand
     */
    fun getSelectedCardBrand(): String? = selectedBrand

    /**
     * Get the list of available brands
     */
    fun getAvailableCardBrands(): List<String> = availableBrands

    /**
     * Get the count of available brand options
     */
    fun getBrandOptionsCount(): Int = availableBrands.size

    /**
     * Set the selected brand programmatically
     */
    fun setSelectedBrand(brandName: String) {
        if (!availableBrands.contains(brandName)) {
            return
        }
        
        selectedBrand = brandName
        text = brandName
        
        // Notify listeners
        sendBrandSelectionEvent(brandName)
        brandSelectionCallback?.invoke(brandName)
    }

    /**
     * Register a callback for brand selection events
     */
    fun onBrandSelection(callback: (String) -> Unit) {
        brandSelectionCallback = callback
    }

    private fun updateAvailableBrands(brands: List<String>) {
        availableBrands = brands
        
        when {
            brands.isEmpty() -> {
                hide()
                text = defaultTitle
                selectedBrand = null
            }
            brands.size == 1 -> {
                hide()
            }
            else -> {
                show()
                if (selectedBrand == null || !brands.contains(selectedBrand)) {
                    text = defaultTitle ?: "Select Card Brand"
                }
            }
        }
    }
    
    private fun show() {
        if (visibility == View.VISIBLE) return
        
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(300)
            .setListener(null)
    }
    
    private fun hide() {
        if (visibility == View.GONE) return
        
        animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                visibility = View.GONE
            }
    }

    private fun showBrandSelectionMenu() {
        if (availableBrands.size <= 1) return
        
        val popup = PopupMenu(context, this)
        
        availableBrands.forEachIndexed { index, brand ->
            popup.menu.add(0, index, index, brand)
        }
        
        popup.setOnMenuItemClickListener { menuItem ->
            val selectedBrandName = availableBrands[menuItem.itemId]
            setSelectedBrand(selectedBrandName)
            true
        }
        
        popup.show()
    }

    private fun sendBrandSelectionEvent(brandName: String) {
        val intent = Intent(ACTION_BRAND_SELECTED).apply {
            putExtra(EXTRA_SELECTED_BRAND, brandName)
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(brandOptionsReceiver)
    }

    companion object {
        const val ACTION_BRAND_OPTIONS_UPDATED = "com.basistheory.elements.BRAND_OPTIONS_UPDATED"
        const val ACTION_BRAND_SELECTED = "com.basistheory.elements.BRAND_SELECTED"
        const val EXTRA_BRAND_OPTIONS = "brand_options"
        const val EXTRA_SELECTED_BRAND = "selected_brand"
    }
}
