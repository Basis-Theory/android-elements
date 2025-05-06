package com.basistheory.elements.util

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.crypto.X25519Encrypter
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jose.util.Base64URL

object EncryptionConstants {
    const val KEY_TYPE = "OKP"
    const val ALGORITHM = "ECDH-ES"
    const val ENCRYPTION_ALGORITHM = "A256GCM"
}

class JWEEncryption(private val publicKey: String) {
    
    fun encrypt(payload: String): String {
        // Create JWK from the base64 encoded public key
        val jwk = OctetKeyPair.Builder(Curve.X25519, Base64URL.from(publicKey))
            .algorithm(Algorithm.parse(EncryptionConstants.ALGORITHM))
            .build()

        // Create JWE header
        val header = JWEHeader.Builder(
            JWEAlgorithm.ECDH_ES,
            com.nimbusds.jose.EncryptionMethod.A256GCM
        ).build()

        // Create encrypter
        val encrypter = X25519Encrypter(jwk)

        // Create JWE object
        val jweObject = com.nimbusds.jose.JWEObject(
            header,
            com.nimbusds.jose.Payload(payload)
        )

        // Encrypt
        jweObject.encrypt(encrypter)

        // Return the compact serialization
        return jweObject.serialize()
    }
}