package com.basistheory.elements.example.view.custom_form

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.basistheory.elements.example.databinding.FragmentCustomFormBinding
import com.basistheory.elements.example.util.tokenExpirationTimestamp
import com.basistheory.elements.example.viewmodel.ApiViewModel
import com.basistheory.elements.model.InputType
import com.basistheory.elements.view.mask.ElementMask

class CustomFormFragment : Fragment() {
    private val binding: FragmentCustomFormBinding by lazy {
        FragmentCustomFormBinding.inflate(layoutInflater)
    }
    private val viewModel: ApiViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        val digitRegex = Regex("""\d""")
        val charRegex = Regex("""[A-Za-z]""")

        binding.name.copyIconColor = Color.BLUE
        binding.name.enableCopy = true

        // illustrates that inputType can be set programmatically (or in xml)
        binding.phoneNumber.inputType = InputType.NUMBER
        binding.phoneNumber.mask = ElementMask(
            listOf(
                "+",
                "1",
                "(",
                digitRegex,
                digitRegex,
                digitRegex,
                ")",
                " ",
                digitRegex,
                digitRegex,
                digitRegex,
                "-",
                digitRegex,
                digitRegex,
                digitRegex,
                digitRegex
            )
        )

        binding.pin.gravity = Gravity.CENTER

        binding.orderNumber.mask = ElementMask(
            listOf(charRegex, charRegex, charRegex, "-", digitRegex, digitRegex, digitRegex)
        )

        binding.tokenizeButton.setOnClickListener { tokenize() }
        binding.autofillButton.setOnClickListener { autofill() }

        return binding.root
    }

    private fun autofill() {
        binding.name.setText("John Doe")
        binding.phoneNumber.setText("2345678900")
        binding.orderNumber.setText("ABC123")
        binding.password.setText("secret password 123")
        binding.pin.setText("1234")
    }

    private fun tokenize() = viewModel.tokenize(object {
        val type = "token"
        val data = object {
            val name = binding.name
            val phoneNumber = binding.phoneNumber
            val orderNumber = binding.orderNumber
            val password = binding.password
            val pin = binding.pin
        }
        val expires_at = tokenExpirationTimestamp()
    }).observe(viewLifecycleOwner) {}
}


