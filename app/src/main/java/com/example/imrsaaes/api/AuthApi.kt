package com.example.imrsaaes.api

import com.example.imrsaaes.model.LoginRequest
import com.example.imrsaaes.model.LoginResponse
import com.example.imrsaaes.model.User
import com.example.imrsaaes.model.getUser
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {
    @POST("/api/auth/createUser")
    fun registerUser(@Body user: User): Call<Void>
    @POST("/api/auth/login")
    fun loginUser(@Body credentials: LoginRequest): Call<LoginResponse>
    @GET("/api/auth/getUser")
    fun getUser(@Header("auth-token") token: String): Call<getUser>
    @GET("/api/auth/getSalt/{email}")
    fun getSalt(@Path("email") email: String): Call<Map<String, String>>
}