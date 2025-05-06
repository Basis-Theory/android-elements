package com.basistheory.elements.util

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWEObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class JWEEncryptionTests {
    
    @Test
    fun testEncryptWithPublicKey() {

        val publicKey = "H+W/XR17FK6y3VfX1rMn26BXNp6W173L+NZXJYkh1mw="
        
        // Create encryption instance with public key
        val encryption = JWEEncryption(publicKey)
        
        // Test payload
        val payload = "Encrypted Text!"
        
        // Encrypt the payload
        val encryptedToken = encryption.encrypt(payload)
        
        // Verify the encrypted token
        assertNotNull("Encrypted token should not be null", encryptedToken)
        
        // Parse the JWE object
        val jweObject = JWEObject.parse(encryptedToken)
        
        // Verify the header
        assertEquals("Algorithm should be ECDH-ES", 
            EncryptionConstants.ALGORITHM, 
            jweObject.header.algorithm.name)
        assertEquals("Encryption method should be A256GCM", 
            EncryptionConstants.ENCRYPTION_ALGORITHM, 
            jweObject.header.encryptionMethod.name)
        assertEquals("Key type should be OKP",
            EncryptionConstants.KEY_TYPE,
            jweObject.header.ephemeralPublicKey.keyType.value)
        
    }
    
    @Test(expected = JOSEException::class)
    fun testEncryptWithInvalidPublicKey() {
        val encryption = JWEEncryption("invalid-key")
        encryption.encrypt("test payload")
    }
}