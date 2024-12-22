package com.example.imrsaaes.model

data class MessageAdapter(
    val _id: String,
    val noty: Boolean,
    var content: String,
    val sender: SenderAdapter,
    val chatId: String,  // Menggunakan tipe String untuk chatId seperti yang diminta
    val encryptedAesKey: String,
    val createdAt: String,
    val updatedAt: String
)

data class SenderAdapter(
    val id: String
)