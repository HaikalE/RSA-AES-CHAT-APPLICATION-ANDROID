package com.example.imrsaaes.repository

import android.content.Context
import android.util.Log
import com.example.imrsaaes.R
import com.example.imrsaaes.api.ChatApi
import com.example.imrsaaes.model.Chat
import com.example.imrsaaes.model.Message
import com.example.imrsaaes.model.MessageRoom
import com.example.imrsaaes.util.CustomDateAdapter
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.Date

class ChatRepository(context: Context) {

    private val chatApi: ChatApi

    init {
        val gson = GsonBuilder()
            .setLenient()  // Add this line
            .registerTypeAdapter(Date::class.java, CustomDateAdapter())
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(context.getString(R.string.url_backend))
            .addConverterFactory(ScalarsConverterFactory.create()) // Tambahkan ini
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        chatApi = retrofit.create(ChatApi::class.java)
    }

    suspend fun fetchRecentChats(token: String): Result<List<Chat>> {
        Log.d("ChatRepository", "Mulai fetchRecentChats dengan token: $token") // Log awal untuk tahu kapan fungsi mulai

        return try {
            val response = chatApi.fetchRecentChats(token)
            Log.d("ChatRepository", "Response diterima dari API: ${response.raw()}") // Log detail response mentah dari API

            if (response.isSuccessful) {
                Log.d("ChatRepository", "Response sukses dengan kode: ${response.code()}") // Log status response sukses
                val body = response.body()
                if (body != null) {
                    Log.d("ChatRepository", "Body response berisi data: ${body.size} chats") // Jumlah data yang diterima
                    body.forEachIndexed { index, chat ->
                        Log.d("ChatRepository", "Chat ke-$index: $chat") // Log detail tiap chat item di body
                    }
                    Result.success(body)
                } else {
                    Log.d("ChatRepository", "Body response kosong") // Jika body null, kasih info
                    Result.success(emptyList())
                }
            } else {
                Log.e("ChatRepository", "Response error dengan kode: ${response.code()}, pesan: ${response.message()}") // Log error kalau gak sukses
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Exception terjadi saat fetchRecentChats: ${e.message}", e) // Log error dengan detail exception
            Result.failure(e)
        }
    }

    fun getMyUserId(chat: Chat, myEmail: String): String? {
        // Mencari user yang memiliki email sesuai dengan myEmail
        return chat.users.find { userWrapper ->
            userWrapper.user.email == myEmail
        }?.user?._id
    }




    suspend fun accessChat(token: String, userTwo: String): Result<Chat> {
        return try {
            Log.d("ChatRepository", "Memulai request untuk accessChat... ke $userTwo") // Debug

            val response = chatApi.accessChat(token, userTwo)
            if (response.isSuccessful) {
                val chat = response.body()
                Log.d("ChatRepository", "Request sukses, chat: $chat") // Debug respons sukses
                Result.success(chat!!)
            } else {
                Log.e("ChatRepository", "Request gagal: ${response.code()} ${response.message()}") // Log error
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Exception terjadi saat accessChat", e) // Log jika ada exception
            Result.failure(e)
        }
    }

    suspend fun getUnseenMessageCount(token: String, chatId: String, userId: String, type: String = "dismiss"): Result<Unit> {
        return try {
            Log.d("ChatRepository", "Starting getUnseenMessageCount - Token: $token, Chat ID: $chatId, User ID: $userId, Type: $type")

            val response = chatApi.getUnseenMessageCount(token, type, chatId, userId)
            Log.d("ChatRepository", "Raw Response: ${response.raw()}")  // Add this for debugging

            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d("ChatRepository", "Response Body: ${response.body()}")
                Log.d("ChatRepository", "Error Body: ${response.errorBody()?.toString()}")

                // Tangani berbagai kemungkinan respons sukses dengan kode 200
                if (responseBody == "Unseen message count reset successfully") {
                    Log.d("ChatRepository", "Unseen message count reset successfully")
                    Result.success(Unit)
                } else {
                    Log.e("ChatRepository", "Unexpected response: $responseBody")
                    Result.failure(Exception("Unexpected response: $responseBody"))
                }
            } else {
                Log.e("ChatRepository", "API Error - Code: ${response.code()}, Message: ${response.message()}, Error Body: ${response.errorBody()?.string()}")
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Exception during getUnseenMessageCount - ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchMessages(token: String, chatId: String): Result<List<Message>> {
        Log.d("ChatRepository", "Fetching messages for chatId: $chatId with token: $token")

        return try {
            val response = chatApi.fetchMessages(token, chatId)

            // Debugging informasi response
            Log.d("ChatRepository", "Response received with code: ${response.code()}")
            Log.d("ChatRepository", "Response body: ${response.body()?.toString() ?: "No response body"}")

            if (response.isSuccessful) {
                // Debug jumlah pesan jika berhasil
                val messages = response.body() ?: emptyList()
                Log.d("ChatRepository", "Fetched ${messages.size} messages successfully")
                Result.success(messages)
            } else {
                // Debug error message jika response tidak berhasil
                Log.e("ChatRepository", "Error response: ${response.code()} ${response.message()}")
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            // Debug exception yang terjadi
            Log.e("ChatRepository", "Exception occurred while fetching messages: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(token: String, content: String, chatId: String, encryptedAesKey: String): Result<MessageRoom> {
        return try {
            Log.d("ChatRepository", "Mengirim pesan ke chatId: $chatId dengan konten: $content")
            Log.d("ChatRepository", "Payload: chatId = $chatId, content = $content, encryptedAesKey = $encryptedAesKey")
            // Persiapkan payload request dengan encryptedAesKey
            val messagePayload = mapOf(
                "content" to content,
                "chatId" to chatId,
                "encryptedAesKey" to encryptedAesKey  // Tambahkan ini
            )

            // Kirim request ke server melalui ChatApi
            val response = chatApi.sendMessage(token, messagePayload)

            // Debugging informasi respons
            Log.d("ChatRepository", "Response diterima dengan kode: ${response.code()}")
            Log.d("ChatRepository", "Body respons: ${response.body()?.toString() ?: "Tidak ada body respons"}")

            if (response.isSuccessful) {
                // Debug pesan berhasil dikirim
                val message = response.body()
                Log.d("ChatRepository", "Pesan terkirim: ${message?.content}")
                Result.success(message!!)
            } else {
                // Debug pesan error jika respons gagal
                Log.e("ChatRepository", "Error respons: ${response.code()} ${response.message()}")
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            // Debug exception yang terjadi
            Log.e("ChatRepository", "Exception terjadi saat mengirim pesan: ${e.message}", e)
            Result.failure(e)
        }
    }




}
