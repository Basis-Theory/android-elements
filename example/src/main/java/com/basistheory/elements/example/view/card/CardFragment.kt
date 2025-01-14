package com.basistheory.elements.example.view.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.basistheory.elements.example.databinding.FragmentCardBinding
import com.basistheory.elements.example.util.tokenExpirationTimestamp
import com.basistheory.elements.example.viewmodel.CardFragmentViewModel

class CardFragment : Fragment() {
    private val binding: FragmentCardBinding by lazy {
        FragmentCardBinding.inflate(layoutInflater)
    }
    private val viewModel: CardFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.cvc.cardNumberElement = binding.cardNumber

        binding.tokenizeButton.setOnClickListener { tokenize() }
        binding.autofillButton.setOnClickListener { autofill() }

        setValidationListeners()

        return binding.root
    }

    private fun autofill() {
        binding.cardNumber.setText("4242424242424242")
        binding.expirationDate.setText("12/25")
        binding.cvc.setText("123")
    }

    private fun tokenize() =
        viewModel.tokenize(
            object {
                val type = "card"
                val data = object {
                    val number = binding.cardNumber
                    val expiration_month = binding.expirationDate.month()
                    val expiration_year = binding.expirationDate.year()
                    val cvc = binding.cvc
                }
                val expires_at = tokenExpirationTimestamp()
            }).observe(viewLifecycleOwner) {}

    /**
     * demonstrates how an application could potentially wire up custom validation behaviors
     */
    private fun setValidationListeners() {
        binding.cardNumber.addChangeEventListener {
            viewModel.cardNumber.observe(it)
        }
        binding.expirationDate.addChangeEventListener {
            viewModel.cardExpiration.observe(it)
        }
        binding.cvc.addChangeEventListener {
            viewModel.cardCvc.observe(it)
        }
    }
}
