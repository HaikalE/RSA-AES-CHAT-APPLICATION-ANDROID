package com.example.imrsaaes.repository


import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.imrsaaes.R
import com.example.imrsaaes.api.AuthApi
import com.example.imrsaaes.model.LoginRequest
import com.example.imrsaaes.model.LoginResponse
import com.example.imrsaaes.model.User
import com.example.imrsaaes.model.getUser
import com.google.gson.GsonBuilder
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AuthRepository(context: Context) {

    private val authApi: AuthApi

    init {
        val gson = GsonBuilder()
            .setLenient() // Terima JSON yang kurang valid
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl(context.getString(R.string.url_backend)) // Memanggil URL dari resource
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        authApi = retrofit.create(AuthApi::class.java)
    }

    // Panggil registerUser untuk kirim data yang sudah dienkripsi dan di-hash
    fun registerUser(user: User): Response<Void>? {
        return try {
            val response: Response<Void> = authApi.registerUser(user).execute()
            if (!response.isSuccessful) {
                Log.e("AuthRepository", "Error: ${response.code()} - ${response.errorBody()?.string()}")
            }
            response
        } catch (e: Exception) {
            Log.e("AuthRepository", "Exception: ${e.message}")
            null
        }
    }

    // Fungsi loginUser untuk otentikasi
    fun loginUser(email: String, password: String): Response<LoginResponse>? {
        return try {
            val salt = getSalt(email)
            if (salt == null) {
                Log.e("AuthRepository", "Salt retrieval failed.")
                return null
            }

            // Hash password dengan salt yang diperoleh dari server
            val hashedPassword = hashPassword(password.toCharArray(), Base64.decode(salt,Base64.DEFAULT))

            // Konversi hashed password ke string Base64
            val hashedPasswordBase64 = Base64.encodeToString(hashedPassword, Base64.DEFAULT)
            Log.e("AuthRepository", "NEW Login hashed password: $hashedPasswordBase64 with salt: ${salt.toByteArray().joinToString()} plain pass : ${password.toCharArray().joinToString()} BYTE ARRAY SALT : ${Base64.encodeToString(Base64.decode(salt,Base64.DEFAULT), Base64.DEFAULT)}, SALT ASLINE : $salt")

            val loginRequest = LoginRequest(email, hashedPasswordBase64)

            val response: Response<LoginResponse> = authApi.loginUser(loginRequest).execute()
            if (!response.isSuccessful) {
                Log.e("AuthRepository", "Login failed with error code: ${response.code()}")
                Log.e("AuthRepository", "Error message: ${response.errorBody()?.string()}")
            } else {
                // Log untuk response body yang berhasil
                val responseBody = response.body()
                Log.d("AuthRepository", "Login successful!")
                Log.d("AuthRepository", "Login Response Body: ${GsonBuilder().setPrettyPrinting().create().toJson(responseBody)}")

                // Jika login menghasilkan token atau data user, log informasinya
                if (responseBody != null) {
                    Log.d("AuthRepository", "User: ${responseBody.user}")
                    Log.d("AuthRepository", "Token: ${responseBody.authToken}")
                }
            }
            response
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login Exception: ${e.message}")
            null
        }
    }

    private fun hashPassword(password: CharArray, salt: ByteArray): ByteArray {
        val iterations = 10000  // pastikan ini sama dengan RegisterActivity
        val keyLength = 256     // pastikan panjang kunci konsisten
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    // Ambil salt dari server berdasarkan email
    private fun getSalt(email: String): String? {
        return try {
            val response = authApi.getSalt(email).execute()
            if (response.isSuccessful) {
                response.body()?.get("salt")
            } else {
                Log.e("AuthRepository", "Error fetching salt: ${response.code()} - ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Exception while fetching salt: ${e.message}")
            null
        }
    }

    fun getUser(token: String): Response<getUser>? {
        Log.d("AuthRepository", "getUser called with token: $token")
        return try {
            val response: Response<getUser> = authApi.getUser(token).execute()

            // Log status kode dan response headers
            Log.d("AuthRepository", "Response status code: ${response.code()}")
            Log.d("AuthRepository", "Response headers: ${response.headers()}")

            // Periksa apakah response berhasil
            if (response.isSuccessful) {
                Log.d("AuthRepository", "Get user successful, response body: ${response.body()}")
            } else {
                Log.e("AuthRepository", "Get user failed with error code: ${response.code()}")
                Log.e("AuthRepository", "Error message: ${response.errorBody()?.string()}")
            }

            response
        } catch (e: Exception) {
            // Log detail dari exception jika terjadi error
            Log.e("AuthRepository", "Get user exception: ${e.message}")
            Log.e("AuthRepository", "Exception stack trace: ${Log.getStackTraceString(e)}")
            null
        }
    }

}
