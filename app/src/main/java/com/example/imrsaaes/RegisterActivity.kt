package com.example.imrsaaes

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imrsaaes.cryptography.AES
import com.example.imrsaaes.cryptography.RSA
import com.example.imrsaaes.model.User
import com.example.imrsaaes.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class RegisterActivity : AppCompatActivity() {

    private lateinit var etNameRegister: EditText
    private lateinit var etEmailRegister: EditText
    private lateinit var etPasswordRegister: EditText
    private lateinit var etConfirmPasswordRegister: EditText
    private lateinit var btnRegister: Button

    private lateinit var authRepository: AuthRepository  // Ubah inisialisasi di sini
    private var generatedSalt: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etNameRegister = findViewById(R.id.etNameRegister)
        etEmailRegister = findViewById(R.id.etEmailRegister)
        etPasswordRegister = findViewById(R.id.etPasswordRegister)
        etConfirmPasswordRegister = findViewById(R.id.etConfirmPasswordRegister)
        btnRegister = findViewById(R.id.btnRegister)
        authRepository = AuthRepository(this)
        btnRegister.setOnClickListener {
            val name = etNameRegister.text.toString().trim()
            val email = etEmailRegister.text.toString().trim()
            val password = etPasswordRegister.text.toString().trim()
            val confirmPassword = etConfirmPasswordRegister.text.toString().trim()

            if (validateInputs(name, email, password, confirmPassword)) {
                Log.d("RegisterActivity", "Starting registration process...")

                val salt = generateAndStoreSalt()

                val hashedPassword = hashPassword(password.toCharArray(), salt)
                Log.d(
                    "RegisterActivity",
                    "Hashed password di register : ${Base64.encodeToString(hashedPassword, Base64.DEFAULT)} " +
                            "with salt : ${salt.joinToString()} with plain PASS : ${password.toCharArray().joinToString()} with PLAIN SALT: ${Base64.encodeToString(salt, Base64.DEFAULT)}"
                )


                val keyPair = RSA.generateKeyPair()
                Log.d("RegisterActivity", "Generated RSA key pair.")

                val rsaPublicJson = JSONObject().apply {
                    put("e", keyPair.publicKey.e)
                    put("n", keyPair.publicKey.n)
                }.toString()
                Log.d("RegisterActivity", "RSA Public Key JSON: $rsaPublicJson")

                val rsaPrivateJson = JSONObject().apply {
                    put("p", keyPair.privateKey.p)
                    put("q", keyPair.privateKey.q)
                    put("d", keyPair.privateKey.d)
                    put("dP", keyPair.privateKey.dP)
                    put("dQ", keyPair.privateKey.dQ)
                    put("qInv", keyPair.privateKey.qInv)
                }.toString()
                Log.d("RegisterActivity", "RSA Private Key JSON: $rsaPrivateJson")

                val aesKey = generateAESKey(password.toCharArray(), salt)
                Log.d("RegisterActivity", "Generated AES key: ${Base64.encodeToString(aesKey, Base64.DEFAULT)}")

                val rsaEncryptedPrivateKey = AES.encrypt(aesKey, rsaPrivateJson)
                Log.d("RegisterActivity", "RSA Private Key (encrypted with AES): ${Base64.encodeToString(rsaEncryptedPrivateKey, Base64.DEFAULT)}")

                // Coba dekripsi kembali untuk memastikan apakah hasil dekripsi cocok dengan original rsaPrivateJson
                val decryptedPrivateKey = AES.decrypt(aesKey, Base64.decode(Base64.encodeToString(rsaEncryptedPrivateKey, Base64.DEFAULT), Base64.DEFAULT))
                Log.d("RegisterActivity", "RSA Private Key (decrypted): $decryptedPrivateKey")

// Validasi apakah hasil dekripsi sama dengan data asli sebelum enkripsi
                if (decryptedPrivateKey == rsaPrivateJson) {
                    Log.d("RegisterActivity", "Decryption successful and matches the original RSA private key JSON ${rsaEncryptedPrivateKey.joinToString()} dengan KEY AES ${aesKey.joinToString()}, dengan SALT ${salt.joinToString()}")
                } else {
                    Log.e("RegisterActivity", "Decryption failed or data mismatch")
                }

                val user = User(
                    name = name,
                    email = email,
                    password = Base64.encodeToString(hashedPassword, Base64.DEFAULT),
                    salt = Base64.encodeToString(salt, Base64.DEFAULT),
                    rsaPublic = rsaPublicJson,
                    rsaEncryptedPrivateKey = Base64.encodeToString(rsaEncryptedPrivateKey, Base64.DEFAULT)
                )
                Log.d("RegisterActivity", "Nih yang gw kirim ya bos : $user")
                registerUser(user)
            }
        }
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPassword: String): Boolean {
        return when {
            name.isEmpty() -> {
                etNameRegister.error = "Nama tidak boleh kosong!"
                etNameRegister.requestFocus()
                false
            }
            email.isEmpty() -> {
                etEmailRegister.error = "Email tidak boleh kosong!"
                etEmailRegister.requestFocus()
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etEmailRegister.error = "Format email tidak valid!"
                etEmailRegister.requestFocus()
                false
            }
//            password.isEmpty() -> {
//                etPasswordRegister.error = "Password tidak boleh kosong!"
//                etPasswordRegister.requestFocus()
//                false
//            }
//            password.length < 8 -> {
//                etPasswordRegister.error = "Password minimal 8 karakter!"
//                etPasswordRegister.requestFocus()
//                false
//            }
            password != confirmPassword -> {
                etConfirmPasswordRegister.error = "Password dan konfirmasi tidak cocok!"
                etConfirmPasswordRegister.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun generateAESKey(password: CharArray, salt: ByteArray): ByteArray {
        val pbKeySpec = PBEKeySpec(password, salt, 65536, 256)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val aesKey = secretKeyFactory.generateSecret(pbKeySpec).encoded
        Log.d("RegisterActivity", "Generated AES Key: ${Base64.encodeToString(aesKey, Base64.DEFAULT)} with SALT : ${salt.joinToString()}")
        return aesKey
    }

    private fun generateAndStoreSalt(): ByteArray {
        if (generatedSalt == null) {
            generatedSalt = ByteArray(16)
            java.security.SecureRandom().nextBytes(generatedSalt)

            // Convert any negative values in generatedSalt to their unsigned equivalents
            val unsignedSalt = generatedSalt!!.map { it.toUByte().toByte() }.toByteArray()

            Log.d("RegisterActivity", "Generated Salt: ${Base64.encodeToString(unsignedSalt, Base64.DEFAULT)}")
            generatedSalt = unsignedSalt
        }
        return generatedSalt!!
    }


    private fun hashPassword(password: CharArray, salt: ByteArray): ByteArray {
        val iterations = 10000
        val keyLength = 256
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hashedPassword = factory.generateSecret(spec).encoded
        Log.d("RegisterActivity", "Hashed Password: ${Base64.encodeToString(hashedPassword, Base64.DEFAULT)}")
        return hashedPassword
    }

    private fun registerUser(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            val response = authRepository.registerUser(user)

            withContext(Dispatchers.Main) {
                if (response != null && response.isSuccessful) {
                    Log.d("RegisterActivity", "Registration successful: ${response.body()}")
                    Toast.makeText(this@RegisterActivity, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                    finish()
                } else if (response != null) {
                    val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("RegisterActivity", "Registration failed: $errorMessage")
                    Toast.makeText(this@RegisterActivity, "Registrasi gagal: $errorMessage", Toast.LENGTH_LONG).show()
                } else {
                    Log.e("RegisterActivity", "Network error during registration.")
                    Toast.makeText(this@RegisterActivity, "Registrasi gagal: Network Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
