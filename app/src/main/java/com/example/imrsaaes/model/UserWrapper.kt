package com.example.imrsaaes.model

data class UserWrapper(
    val user: UserChat,
    val unseenMsg: Int,
    val _id: String
)