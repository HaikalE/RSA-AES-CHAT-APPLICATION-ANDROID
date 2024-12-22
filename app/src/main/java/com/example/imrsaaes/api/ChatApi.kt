package com.example.imrsaaes.api

import com.example.imrsaaes.model.Chat
import com.example.imrsaaes.model.Message
import com.example.imrsaaes.model.MessageRoom
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ChatApi {

    @GET("/api/chat/fetchChats")
    suspend fun fetchRecentChats(
        @Header("auth-token") token: String
    ): Response<List<Chat>>

    @GET("/api/chat/accessChat")
    suspend fun accessChat(
        @Header("auth-token") token: String,
        @retrofit2.http.Query("userTwo") userTwo: String
    ): Response<Chat>

    @GET("/api/chat/fetchMessages")
    suspend fun fetchMessages(
        @Header("auth-token") token: String,
        @Query("Id") chatId: String
    ): Response<List<Message>>

    @GET("/api/chat/countMssg")
    suspend fun getUnseenMessageCount(
        @Header("auth-token") token: String,
        @Query("type") type: String = "dismiss",
        @Query("chatId") chatId: String,
        @Query("userId") userId: String
    ): Response<String>

    // Tambahkan fungsi untuk mengirim pesan di dalam interface ChatApi

    @retrofit2.http.POST("/api/chat/message")
    suspend fun sendMessage(
        @Header("auth-token") token: String,
        @retrofit2.http.Body messagePayload: Map<String, String>  // atau gunakan model sesuai kebutuhan
    ): Response<MessageRoom>

}
