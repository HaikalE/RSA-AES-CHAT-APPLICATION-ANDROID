package com.example.imrsaaes.model

data class ChatId(
    val _id: String,
    val isGroupChat: Boolean,
    val users: List<User>,
    val profilePic: String?,
    val createdAt: String,
    val updatedAt: String,
    val __v: Int,
    val latestMessage: String
)