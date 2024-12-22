package com.example.imrsaaes.api

import com.example.imrsaaes.model.SearchUser
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface UserApi {
    @GET("api/chat/searchUser")
    suspend fun searchUsers(
        @Query("search") query: String,
        @Header("auth-token") token: String  // Tambahkan token di header
    ): Response<List<SearchUser>>
}