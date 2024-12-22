package com.example.imrsaaes.model

data class LatestMessage(
    val _id: String,
    val chatId: String,  // Ubah menjadi String untuk menampung ObjectId sebagai string
    val content: String,
    val sender: Sender,
    val createdAt: String,
    val updatedAt: String,
    val __v: Int
)
