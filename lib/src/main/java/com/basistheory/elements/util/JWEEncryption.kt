package com.basistheory.elements.util

import com.google.gson.Gson
import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.crypto.X25519Encrypter
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.util.Base64URL

object JWEEncryption {
    
    fun encrypt(payload: Any, publicKey: String, keyId: String): String {
        // Remove PEM format if present
        val cleanedPublicKey = removePemFormat(publicKey)

        if (keyId.isEmpty()) {
            throw IllegalArgumentException("Key ID is required")
        }
        
        // Create JWK from the base64 encoded public key
        val jwk = OctetKeyPair.Builder(Curve.X25519, Base64URL.from(cleanedPublicKey))
            .algorithm(Algorithm.parse("ECDH-ES"))
            .keyID(keyId)
            .build()


        // Create JWE header
        val header = JWEHeader.Builder(
            JWEAlgorithm.ECDH_ES,
            com.nimbusds.jose.EncryptionMethod.A256GCM)
            .keyID(keyId)
            .build()

        // Create encrypter
        val encrypter = X25519Encrypter(jwk)

        val gson = Gson()
        val payloadJson = payload as? String ?: gson.toJson(payload)

        // Create JWE object
        val jweObject = com.nimbusds.jose.JWEObject(
            header,
            com.nimbusds.jose.Payload(payloadJson)
        )

        // Encrypt
        jweObject.encrypt(encrypter)

        // Return the compact serialization
        return jweObject.serialize()
    }

    /**
     * Removes PEM format headers, footers, and whitespace from a public key string.
     * Handles formats like:
     * -----BEGIN PUBLIC KEY-----
     * [base64 content]
     * -----END PUBLIC KEY-----
     */
    internal fun removePemFormat(publicKey: String): String {
        return publicKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace(Regex("\\s"), "") // Remove all whitespace including newlines
            .trim()
    }
}