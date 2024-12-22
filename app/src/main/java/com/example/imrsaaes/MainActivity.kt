package com.example.imrsaaes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.imrsaaes.cryptography.AES

class MainActivity : AppCompatActivity() {

    lateinit var btnGoToLogin: Button
    lateinit var btnGoToRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Cek apakah auth token ada di SharedPreferences
        val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val authToken = sharedPref.getString("authToken", null)

        // Jika auth token ada, langsung ke DashboardActivity
        if (authToken != null) {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish() // Tutup MainActivity agar tidak bisa kembali ke sini setelah login
        }

        // Inisialisasi tombol jika token tidak ada
        btnGoToLogin = findViewById(R.id.btnGoToLogin)
        btnGoToRegister = findViewById(R.id.btnGoToRegister)

        btnGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        btnGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Tambahkan logika AES encryption/decryption sebagai tes
        testAesEncryptionDecryption()
    }

    private fun testAesEncryptionDecryption() {
        Log.d("AES", "=== Testing AES Encryption and Decryption ===")

        // Hardcoded AES Key
        val aesKey = byteArrayOf(
            111, -94, 27, 111, 122, -112, 1, -67, 91, 94, 12, -127, -104, 27, 95, 6,
            22, 6, 12, 18, -120, -28, -64, 39, 118, 103, 91, -96, -126, -97, -84, -125
        )
        Log.d("AES", "Using predefined AES Key: ${aesKey.joinToString("") { "%02x".format(it) }}")

        // Define plaintext to encrypt
        val plaintext = "Hello AES Encryption!"
        Log.d("AES", "Plaintext: $plaintext")

        try {
            // Encrypt the plaintext
            val encryptedData = AES.encrypt(aesKey, plaintext)
            Log.d("AES", "Encrypted Data (Hex): ${encryptedData.joinToString("") { "%02x".format(it) }}")

            // Decrypt the encrypted data
            val decryptedText = AES.decrypt(aesKey, encryptedData)
            Log.d("AES", "Decrypted Text: $decryptedText")

            // Verify if the decrypted text matches the original plaintext
            if (plaintext.trim() == decryptedText.trim()) {
                Log.d("AES", "Test Passed: Decrypted text matches the original plaintext!")
            } else {
                Log.d("AES", "Test Failed: Decrypted text does not match the original plaintext.")
            }
        } catch (e: Exception) {
            Log.e("AES", "Error during AES encryption/decryption: ${e.message}", e)
        }
    }
}
