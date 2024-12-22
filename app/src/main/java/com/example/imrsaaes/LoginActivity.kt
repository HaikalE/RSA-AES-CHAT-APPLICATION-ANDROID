package com.example.imrsaaes

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imrsaaes.cryptography.AES
import com.example.imrsaaes.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class LoginActivity : AppCompatActivity() {

    lateinit var etEmail: EditText
    lateinit var etPassword: EditText
    lateinit var btnLogin: Button
    lateinit var tvRegister: TextView
    private lateinit var authRepository: AuthRepository  // Ubah inisialisasi di sini

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)
        authRepository = AuthRepository(this)
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            // Validate input
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password harus diisi!", Toast.LENGTH_SHORT).show()
            } else {
                loginUser(email, password)
            }
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser(email: String, password: String) {
        // Gunakan coroutine untuk menangani panggilan jaringan di background thread
        CoroutineScope(Dispatchers.IO).launch {
            val response = authRepository.loginUser(email, password)

            withContext(Dispatchers.Main) {
                if (response != null && response.isSuccessful) {
                    val loginResponse = response.body()

                    if (loginResponse != null) {
                        Log.d("LoginActivity", "Auth token diterima: ${loginResponse.authToken}")

                        // Simpan token autentikasi di SharedPreferences
                        Toast.makeText(this@LoginActivity, "Login sukses!", Toast.LENGTH_SHORT).show()

                        val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("authToken", loginResponse.authToken)  // Simpan token
                            putString("email", email)  // Simpan email
                            apply()
                        }

                        // Ambil ulang token untuk memastikan sudah tersimpan
                        val savedToken = sharedPref.getString("authToken", null)
                        Log.d("LoginActivity", "Auth token disimpan: $savedToken")

                        // Panggil fungsi getUser untuk mengambil data user
                        getUser(loginResponse.authToken!!,password)
                    } else {
                        Toast.makeText(this@LoginActivity, "Login gagal, coba lagi!", Toast.LENGTH_SHORT).show()
                        Log.d("LoginActivity", "Login gagal, response body null")
                    }

                } else {
                    Toast.makeText(this@LoginActivity, "Login gagal, periksa kembali email dan password!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateAESKey(password: CharArray, salt: ByteArray): ByteArray {
        val pbKeySpec = PBEKeySpec(password, salt, 65536, 256)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val aesKey = secretKeyFactory.generateSecret(pbKeySpec).encoded
        // Simpan kunci AES ke SharedPreferences
        val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("aesKey", Base64.encodeToString(aesKey, Base64.DEFAULT))  // Simpan kunci AES sebagai Base64
            apply()
        }

        Log.d("LoginActivity", "Generated AES Key: ${Base64.encodeToString(aesKey, Base64.DEFAULT)} with SALT : ${salt.joinToString()}")
        return aesKey
    }



    private fun decryptRsaPrivateKey(encryptedPrivateKey: String, password: String, salt: ByteArray): String? {
        return try {
            // Decode the encrypted private key from Base64
            val aesKey = generateAESKey(password.toCharArray(), salt)
            Log.d("LoginActivity", "GENERATE AES KEY: ${aesKey.joinToString()}")
            val encryptedData = Base64.decode(encryptedPrivateKey, Base64.DEFAULT)

            // Decrypt the RSA private key
            val decryptedPrivateKey = AES.decrypt(aesKey, encryptedData)

            if (decryptedPrivateKey != null) {
                // Save decrypted RSA Private Key in SharedPreferences
                val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("rsaPrivateKey", decryptedPrivateKey)
                    apply()
                }
                Log.d("LoginActivity", "RSA Private Key disimpan: $decryptedPrivateKey")
            }
            Log.d("LoginActivity", "Decrypted RSA Private Key: $decryptedPrivateKey")
            decryptedPrivateKey
        } catch (e: Exception) {
            Log.e("LoginActivity", "Failed to decrypt RSA private key: ${e.message} ini dia bytearray : ${Base64.decode(encryptedPrivateKey, Base64.DEFAULT).joinToString()}")
            null
        }
    }


    private fun getUser(authToken: String, password: String) {
        Log.d("LoginActivity", "getUser dipanggil dengan token: $authToken")

        CoroutineScope(Dispatchers.IO).launch {
            val response = authRepository.getUser(authToken)

            withContext(Dispatchers.Main) {
                if (response != null) {
                    Log.d("LoginActivity", "Response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val user = response.body()
                        Log.d("LoginActivity", "User response: $user")

                        if (user != null) {
                            // Dekripsi kunci privat RSA setelah mendapatkan data user
                            val encryptedPrivateKey = user.rsaEncryptedPrivateKey
                            val idUser = user._id;
                            val name = user.name;
                            // Simpan userId ke SharedPreferences
                            val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("userId", idUser)  // Simpan userId
                                putString("userName",name)
                                apply()
                            }
                            val salt = Base64.decode(user.salt,Base64.DEFAULT)

                            if (encryptedPrivateKey != null && salt != null) {
                                val decryptedPrivateKey = decryptRsaPrivateKey(user.rsaEncryptedPrivateKey, password, salt)
                                if (decryptedPrivateKey != null) {
                                    Log.d("LoginActivity", "Kunci privat RSA berhasil didekripsi: $decryptedPrivateKey")
                                } else {
                                    Log.e("LoginActivity", "Gagal mendekripsi kunci privat RSA! encrypt privatekey: ${Base64.decode(encryptedPrivateKey, Base64.DEFAULT).joinToString()},salt: ${salt.joinToString()}, password: $password")
                                }
                            } else {
                                Log.e("LoginActivity", "Kunci privat RSA terenkripsi atau salt tidak ditemukan!")
                            }

                            // Pindah ke Dashboard dan bawa data user
                            val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                            intent.putExtra("USER_NAME", user.name)
                            intent.putExtra("USER_EMAIL", user.email)
                            startActivity(intent)
                            finish()
                        } else {
                            Log.e("LoginActivity", "Gagal mengambil data user: User null")
                            Toast.makeText(this@LoginActivity, "Gagal mengambil data user!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("LoginActivity", "Request gagal: ${response.code()} - ${response.errorBody()?.string()}")
                        Toast.makeText(this@LoginActivity, "Gagal login. Tidak bisa mengambil data user!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("LoginActivity", "Response null saat mengambil data user")
                    Toast.makeText(this@LoginActivity, "Gagal login. Tidak bisa mengambil data user!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}
