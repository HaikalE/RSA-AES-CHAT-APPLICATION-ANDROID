package com.example.imrsaaes.model

import java.util.Date

data class Chat(
    val _id: String, // MongoDB ID
    val chatName: String?, // Optional name for the chat
    val users: List<UserWrapper>, // List of users with unseen message count
    val profilePic: String = "https://cdn6.aptoide.com/imgs/1/2/2/1221bc0bdd2354b42b293317ff2adbcf_icon.png", // Default profile picture
    val latestMessage: Message? = null, // Make latestMessage nullable to handle missing data
    val createdAt: Date,
    val updatedAt: Date
)

