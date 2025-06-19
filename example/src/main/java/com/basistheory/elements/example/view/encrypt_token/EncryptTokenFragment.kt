package com.basistheory.elements.example.view.encrypt_token

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.basistheory.elements.example.databinding.FragmentEncryptTokenBinding
import com.basistheory.elements.example.viewmodel.CardFragmentViewModel
import com.basistheory.elements.model.EncryptTokenRequest

class EncryptTokenFragment : Fragment() {
    private val binding: FragmentEncryptTokenBinding by lazy {
        FragmentEncryptTokenBinding.inflate(layoutInflater)
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

        binding.encryptButton.setOnClickListener { encryptToken() }
        binding.autofillButton.setOnClickListener { autofill() }

        setValidationListeners()

        return binding.root
    }

    private fun autofill() {
        binding.cardNumber.setText("4242424242424242")
        binding.expirationDate.setText("12/25")
        binding.cvc.setText("123")
    }

    private fun encryptToken() =
        viewModel.encryptToken(EncryptTokenRequest(
            tokenRequests = object {
                val data = object {
                    val number = binding.cardNumber
                    val expiration_month = binding.expirationDate.month()
                    val expiration_year = binding.expirationDate.year()
                    val cvc = binding.cvc
                }
                val type = "card"
            },
            publicKey = "-----BEGIN PUBLIC KEY-----\nw6RFs74UmOcxjbWBSlZQ0QLam63bvKQvGeLSVfxYIR8=\n-----END PUBLIC KEY-----",
            keyId = "d6b86549-212f-4bdc-adeb-2f39902740f6"
        )).observe(viewLifecycleOwner) {}
    

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
