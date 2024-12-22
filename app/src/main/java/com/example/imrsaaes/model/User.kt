package com.example.imrsaaes.model


data class User(
    val name: String,
    val email: String,
    val password: String,
    val salt:String,
    val rsaPublic:String,
    val rsaEncryptedPrivateKey:String
)
