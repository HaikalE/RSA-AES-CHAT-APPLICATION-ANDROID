package com.example.imrsaaes.model

data class Message(
    val _id: String,
    val noty: Boolean,
    var content: String,
    val sender: Sender,
    val chatId: String,  // Pastikan hanya ada satu deklarasi chatId dengan tipe ChatId
    val encryptedAesKey:String,
    val createdAt: String,
    val updatedAt: String
)