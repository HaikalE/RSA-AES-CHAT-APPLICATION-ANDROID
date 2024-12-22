package com.example.imrsaaes.model


data class getUser(
    val _id:String,
    val name: String,
    val email: String,
    val password: String,
    val salt:String,
    val rsaPublic:String,
    val rsaEncryptedPrivateKey:String
)
