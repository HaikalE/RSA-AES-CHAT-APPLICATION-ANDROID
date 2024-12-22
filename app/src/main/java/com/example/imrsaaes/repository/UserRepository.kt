// UserRepository.kt
package com.example.imrsaaes.repository


import android.content.Context
import android.util.Log
import com.example.imrsaaes.R
import com.example.imrsaaes.api.UserApi
import com.example.imrsaaes.model.SearchUser
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class UserRepository(context: Context) {

    private val userApi: UserApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(context.getString(R.string.url_backend))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        userApi = retrofit.create(UserApi::class.java)
    }

    suspend fun searchUsers(query: String, token: String): Result<List<SearchUser>> {
        return try {
            Log.d("UserRepository", "Memulai pencarian pengguna untuk query: $query") // Debug

            val response = userApi.searchUsers(query, token)
            if (response.isSuccessful) {
                val users = response.body() ?: emptyList()
                Log.d("UserRepository", "Pencarian sukses, jumlah pengguna ditemukan: ${users.size}") // Debug sukses
                Result.success(users)
            } else {
                Log.e("UserRepository", "Pencarian gagal: ${response.code()} ${response.message()} ini tokennya $token") // Log error
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Exception terjadi saat pencarian pengguna", e) // Log jika ada exception
            Result.failure(e)
        }
    }
}
