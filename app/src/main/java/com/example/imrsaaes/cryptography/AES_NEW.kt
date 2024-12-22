package com.example.imrsaaes.cryptography

import android.util.Log
import com.example.imrsaaes.cryptography.core.AES
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AES{

    private var cipher: AES? = null

    // Fungsi untuk mengonversi kunci String menjadi ByteArray dengan PBKDF2

    // Fungsi untuk enkripsi menggunakan ECB
    fun encrypt(aesKey: ByteArray, data: String): ByteArray {
        Log.d("EncryptDebug", "Starting encryption process...")

        // Log the AES key (convert to hex for readability)
        Log.d("EncryptDebug", "AES Key: ${aesKey.joinToString("") { "%02x".format(it) }}")

        // Initialize the cipher
        try {
            cipher = AES(aesKey)
            Log.d("EncryptDebug", "Cipher initialized successfully.")
        } catch (e: Exception) {
            Log.e("EncryptDebug", "Error initializing cipher: ${e.message}", e)
            throw e
        }

        // Fill the block
        val filledText: ByteArray
        try {
            filledText = fillBlock(data).toByteArray()
            Log.d("EncryptDebug", "Text after fillBlock: ${String(filledText)}")
        } catch (e: Exception) {
            Log.e("EncryptDebug", "Error during fillBlock: ${e.message}", e)
            throw e
        }

        // Encrypt the data
        return try {
            val encryptedData = cipher!!.ECB_encrypt(filledText)
            Log.d("EncryptDebug", "Encryption successful. Encrypted data: ${encryptedData.joinToString("") { "%02x".format(it) }}")
            encryptedData
        } catch (e: Exception) {
            Log.e("EncryptDebug", "Error during encryption: ${e.message}", e)
            throw e
        }
    }

    fun decrypt(aesKey: ByteArray, encryptedData: ByteArray): String {
        Log.d("AES_DEBUG", "=== Decrypt Function ===")
        Log.d("AES_DEBUG", "AES Key (Hex): ${aesKey.joinToString("") { "%02x".format(it) }}")
        Log.d("AES_DEBUG", "Encrypted Data (Hex): ${encryptedData.joinToString("") { "%02x".format(it) }}")

        // Initialize cipher
        cipher = AES(aesKey)
        Log.d("AES_DEBUG", "Cipher initialized with AES Key.")

        // Perform decryption
        val decryptedData = cipher!!.ECB_decrypt(encryptedData)
        Log.d("AES_DEBUG", "Decrypted Data (Byte Array): ${decryptedData.joinToString("") { "%02x".format(it) }}")

        // Convert to string and trim padding
        val result = String(decryptedData).trim()
        Log.d("AES_DEBUG", "Decrypted String (Trimmed): $result")

        return result
    }


    // Mengisi teks hingga menjadi kelipatan blok 16 byte
    private fun fillBlock(text: String): String {
        var text = text
        val spaceNum = if (text.toByteArray().size % 16 == 0) 0 else 16 - text.toByteArray().size % 16
        for (i in 0 until spaceNum) text += " "
        return text
    }

    // Fungsi untuk menghasilkan kunci AES dari password dan salt
    fun generateAESKey(password: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        return factory.generateSecret(spec).encoded
    }

    // Fungsi untuk menghasilkan salt secara acak
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }
}
