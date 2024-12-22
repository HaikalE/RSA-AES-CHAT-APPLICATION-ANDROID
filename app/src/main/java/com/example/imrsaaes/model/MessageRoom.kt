package com.example.imrsaaes.model

data class MessageRoom(
    val _id: String,
    val noty: Boolean,
    val content: String,
    val sender: Sender,
    val chatId: ChatId,  // Pastikan hanya ada satu deklarasi chatId dengan tipe ChatId
    val encryptedAesKey: String,  // Tambahkan ini
    val createdAt: String,
    val updatedAt: String
) {
    fun toMessage(): Message {
        return Message(
            _id = _id,
            noty = noty,
            content = content,
            sender = sender,
            chatId = chatId._id, // Ambil ID sebagai string dari objek ChatId
            encryptedAesKey = encryptedAesKey,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

}