package com.example.imrsaaes.model

data class UserDetail(
    val _id: String,
    val name: String,
    val email: String,
    val password: String,
    val isGuest: Boolean,
    val __v: Int
)