package com.basistheory.elements.example.view.raw_proxy_response

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.basistheory.elements.example.databinding.FragmentRawProxyResponseBinding
import com.basistheory.elements.example.viewmodel.ApiViewModel
import com.basistheory.elements.service.ProxyRequest

class RawProxyResponseFragment : Fragment() {
    private val binding: FragmentRawProxyResponseBinding by lazy {
        FragmentRawProxyResponseBinding.inflate(layoutInflater)
    }
    private val viewModel: ApiViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.invokeProxyButton.setOnClickListener { invokeProxy() }
        binding.autofillButton.setOnClickListener { autofill() }

        return binding.root
    }

    private fun autofill() {
        binding.textElement.setText("This is a test")
    }

    private fun invokeProxy() {
        val proxyRequest: ProxyRequest = ProxyRequest().apply {
            headers = mapOf(
                "BT-PROXY-KEY" to "key_dev_prod_us_proxy_BruT4udzdCSYbWkMoMsR6V",
                "Content-Type" to "application/json"
            )
            body = object {
                val text = binding.textElement
                // echoed back in response to mimic proxy responses containing arrays
                val array = arrayListOf<Any>(
                    object {
                        val id = 1
                        val description = "value"
                    }
                )
            }
        }

        viewModel.proxy(proxyRequest).observe(viewLifecycleOwner) { }

    }
}
