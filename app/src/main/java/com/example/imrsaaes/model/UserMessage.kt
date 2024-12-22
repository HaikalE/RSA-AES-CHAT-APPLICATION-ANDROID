package com.example.imrsaaes.model

data class UserMessage(
    val user: UserDetail,
    val unseenMsg: Int,
    val _id: String
)