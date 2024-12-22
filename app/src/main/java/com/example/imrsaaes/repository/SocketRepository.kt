package com.example.imrsaaes.repository

import android.util.Log
import com.example.imrsaaes.api.SocketApi
import io.socket.client.Socket
import org.json.JSONObject

class SocketRepository {

    private var socket: Socket? = SocketApi.getSocketInstance()

    init {
        connectSocket()
    }

    fun setupUser(chatId: String?,userId: String?) {
        val chatData = JSONObject().apply {
            put("chatId", chatId)
            put("userId", userId)
        }
        socket?.emit("setup", chatData)
        Log.d("SocketRepository", "Setup event sent with chat data: $chatData")
    }

    private fun connectSocket() {
        socket?.on(Socket.EVENT_CONNECT) {
            Log.d("SocketRepository", "Socket connected")
        }?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("SocketRepository", "Connection error: ${args[0]}")
        }
        socket?.connect()
        Log.d("SocketRepository", "Attempting to connect: ${socket?.connected()}")
    }

    fun disconnectSocket() {
        socket?.disconnect()
        Log.d("SocketRepository", "Socket disconnected")
    }

    fun listenForMessages(onMessageReceived: (JSONObject) -> Unit) {
        socket?.on("message") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                Log.d("SocketRepository", "Message received: $data")
                onMessageReceived(data)
            }
        }
    }

    fun disconnect() {
        socket?.disconnect() // Putus koneksi socket
        Log.d("SocketRepository", "Socket disconnected successfully")
    }

    // Send typing status
    fun sendTypingStatus(chatId: String, userId: String) {
        val typingData = JSONObject().apply {
            put("chatId", chatId)
            put("user", JSONObject().put("_id", userId))
        }
        socket?.emit("toggleTyping", typingData)
        Log.d("SocketRepository", "Typing status sent: $typingData")
    }

    // Listen for typing status
    fun receiveTypingStatus(onTypingStatus: (JSONObject) -> Unit) {
        socket?.on("isTyping") { args ->
            if (args.isNotEmpty() && args[0] is JSONObject) {
                val data = args[0] as JSONObject
                Log.d("SocketRepository", "Typing status received: $data")
                onTypingStatus(data)
            } else {
                Log.e("SocketRepository", "isTyping event received but no data present")
            }
        }
    }

    // Send message
    fun sendMessage(chatId: String, content: String, encryptedAesKey: String, senderId: String) {
        val messageData = JSONObject().apply {
            put("chatId", chatId)
            put("content", content)
            put("encryptedAesKey", encryptedAesKey)
            put("senderId", senderId)  // Kirim senderId langsung
        }
        socket?.emit("sendMessage", messageData)
        Log.d("SocketRepository", "Message sent over socket: $messageData")
    }




}
