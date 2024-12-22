package com.example.imrsaaes.model


data class LoginResponse(
    val authToken: String,
    val user: User
)