package com.example.imrsaaes.model

data class UserChat(
    val _id: String,
    val name: String,
    val email: String,
    val password: String,
    val rsaPublic: String,
    val isGuest: Boolean
)