package com.example.imrsaaes.cryptography


import android.util.Log
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


object AES_OLD {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128


    // Fungsi untuk mengonversi kunci String menjadi ByteArray dengan PBKDF2
    fun generateAESKey(password: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        return factory.generateSecret(spec).encoded
    }


    // Fungsi untuk mengenkripsi data menggunakan AES
    fun encrypt(aesKey: ByteArray, data: String): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt) // Generate salt (optional, bisa skip kalau ga dipakai di dekripsi)


        val secretKey = SecretKeySpec(aesKey, "AES") // Langsung pakai aesKey yang sudah ada
        val cipher = Cipher.getInstance(TRANSFORMATION)


        // Buat IV baru
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(TAG_SIZE, iv)


        // Inisialisasi cipher untuk mode enkripsi
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)


        // Enkripsi data
        val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))


        // Gabungkan IV, salt, dan data terenkripsi
        return iv + salt + encryptedData
    }




    fun decrypt(aesKey: ByteArray, encryptedData: ByteArray): String {
        Log.d("DecryptFunction", "Starting decryption process")


        // Mendapatkan IV, salt, dan data terenkripsi
        val iv = encryptedData.copyOfRange(0, IV_SIZE)
        val salt = encryptedData.copyOfRange(IV_SIZE, IV_SIZE + 16)
        val data = encryptedData.copyOfRange(IV_SIZE + 16, encryptedData.size)
        Log.d("DecryptFunction", "Extracted IV: ${iv.contentToString()}")
        Log.d("DecryptFunction", "Extracted salt: ${salt.contentToString()}")
        Log.d("DecryptFunction", "Extracted encrypted data: ${data.contentToString()}")


        // Menggunakan aesKey langsung tanpa regenerasi
        val secretKey = SecretKeySpec(aesKey, "AES")
        Log.d("DecryptFunction", "Using pre-generated AES key: ${aesKey.joinToString(", ")}")


        // Menyiapkan cipher untuk dekripsi
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        Log.d("DecryptFunction", "Cipher initialized for decryption")


        // Melakukan dekripsi dan mengonversi hasil ke string
        val decryptedData = cipher.doFinal(data)
        val result = String(decryptedData, Charsets.UTF_8)
        Log.d("DecryptFunction", "Decryption successful, result: $result")


        return result
    }
}
