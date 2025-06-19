package com.basistheory.elements.util

import com.google.gson.Gson
import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.JWEObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.X25519Encrypter
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.util.Base64URL

object JWEEncryption {
    
    fun encrypt(payload: Any, publicKey: String, keyId: String): String {

        require(!keyId.isEmpty()) { "Key ID is required" }

        // Create JWK from the base64 encoded public key
        val jwk = OctetKeyPair
            .Builder(Curve.X25519, Base64URL.from(publicKey.removePemFormat()))
            .algorithm(Algorithm.parse("ECDH-ES"))
            .keyID(keyId)
            .build()


        // Create JWE header
        val header = JWEHeader.Builder(
            JWEAlgorithm.ECDH_ES,
            com.nimbusds.jose.EncryptionMethod.A256GCM)
            .keyID(keyId)
            .build()

        val payloadJson = payload as? String ?: Gson().toJson(payload)

        // Create JWE object
        return JWEObject(header,Payload(payloadJson)).apply {
            encrypt(X25519Encrypter(jwk))
        }.serialize()
    }

    /**
     * Removes PEM format headers, footers, and whitespace from a public key string.
     * Handles formats like:
     * -----BEGIN PUBLIC KEY-----
     * [base64 content]
     * -----END PUBLIC KEY-----
     */
    private fun String.removePemFormat(): String =
        replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replace(Regex("\\s"), "") // Remove all whitespace including newlines
        .trim()

}