package com.example.imrsaaes

import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imrsaaes.cryptography.AES
import com.example.imrsaaes.cryptography.RSA
import com.example.imrsaaes.model.Message
import com.example.imrsaaes.model.Sender
import com.example.imrsaaes.repository.ChatRepository
import com.example.imrsaaes.repository.SocketRepository
import com.example.imrsaaes.util.ChatAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var aesKey: ByteArray
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var socketRepository: SocketRepository // Instance SocketRepository
    var aesKeyEncryptionDuration: Double = 0.0
    var messageEncryptionDuration: Double = 0.0
    var aesKeyDecryptionDuration: Double = 0.0
    var messageDecryptionDuration: Double = 0.0

    private fun encryptAesKey(aesKeyBase64: String, rsaPublicKey: String): String {
        val startTime = System.nanoTime()
        val encryptedAesKey = RSA.encrypt(aesKeyBase64, rsaPublicKey)
        val endTime = System.nanoTime()

        val duration = (endTime - startTime) / 1_000_000.0
        Log.d("EncryptionTime", "AES key encryption time: $duration ms")

        return encryptedAesKey.joinToString(",") { it.toString() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        val chatId = intent.getStringExtra("CHAT_ID")
        val userId = intent.getStringExtra("USER_ID")
        val rsaPublic = intent.getStringExtra("RSA_PUBLIC_KEY")

        val chatUserNameTextView = findViewById<TextView>(R.id.chatUserName)
        chatUserNameTextView.text = "Chat with $userName"

        val chatRecyclerView = findViewById<RecyclerView>(R.id.chatRecyclerView)
        chatRecyclerView.layoutManager = LinearLayoutManager(this)

        val chatInput = findViewById<EditText>(R.id.chatInput)
        val sendButton: ImageButton = findViewById(R.id.sendButton)

        // Tambahkan Handler untuk mengatur timeout status typing


        loadAESKey()
        socketRepository = SocketRepository()
        // Initialize Socket and setup user
        // Ambil userId dari SharedPreferences dan gunakan di setupUser
        chatId?.let { id -> socketRepository.setupUser(id, null) }



        sendButton.setOnClickListener {
            val message = chatInput.text.toString().trim()
            if (message.isNotEmpty()) {
                rsaPublic?.let { rsaKey ->
                    val aesKeyBase64 = Base64.encodeToString(aesKey, Base64.DEFAULT)
//                    val encryptedAesKey = RSA.encrypt(aesKeyBase64, rsaKey)
                    val encryptedAesKeyString = encryptAesKey(aesKeyBase64,rsaKey)
                    Log.d("ChatDebug", "AES KEY yang di encode : $aesKeyBase64")
//                    val encryptedAesKeyString = encryptedAesKey.joinToString(",") { it.toString() }
                    val encryptedMessage = encryptMessage(message)
                    sendMessage(encryptedMessage, encryptedAesKeyString)
                    decryptMessage(encryptedMessage)
                    chatInput.text.clear()
                } ?: run { chatInput.error = "Public key is required" }
            } else {
                chatInput.error = "Message cannot be empty"
            }
        }

        socketRepository.listenForMessages { messageJson ->
            runOnUiThread {
                try {
                    Log.d("ChatDebug", "Received message JSON: $messageJson")

                    // Get the current chat ID from intent to match with incoming message's chatId
                    val currentChatId = intent.getStringExtra("CHAT_ID") ?: ""

                    // Cek dan ambil elemen-elemen yang dibutuhkan dengan pengecekan keberadaan
                    val encryptedContent = messageJson.optString("content", "")
                    val encryptedAesKey = messageJson.optString("encryptedAesKey", "")
                    val senderJson = messageJson.optJSONObject("sender")
                    val senderId = senderJson?.optString("_id", "") ?: ""
                    val messageChatId = messageJson.optString("chatId", "")
                    val userId = intent.getStringExtra("USER_ID")
                    Log.d("ChatDebug", "Content: $encryptedContent, EncryptedAesKey: $encryptedAesKey, Sender ID: $senderId, Message Chat ID: $messageChatId")

                    // Check if the message's chatId matches the current chatId
                    if (messageChatId == currentChatId && senderId!=userId && encryptedContent.isNotEmpty() && encryptedAesKey.isNotEmpty() && senderId.isNotEmpty()) {
                        // Proceed with decryption if chatId matches
                        val content = decryptMessageContent(encryptedContent, encryptedAesKey, senderId, isFetchMessage = false)
                        Log.d("ChatDebug", "Decrypted Content: $content")

                        // Create Message object
                        val message = Message(
                            _id = messageJson.optString("_id", "unknown_id").also { Log.d("ChatDebug", "Message ID: $it") },
                            chatId = messageChatId,
                            content = content,
                            sender = Sender(
                                _id = senderId.also { Log.d("ChatDebug", "Sender ID: $it") },
                                name = senderJson?.optString("name", "Unknown") ?: "Unknown",
                                email = senderJson?.optString("email", "Unknown") ?: "Unknown",
                                isGuest = senderJson?.optBoolean("isGuest", false) ?: false
                            ),
                            createdAt = messageJson.optString("createdAt", "unknown_date"),
                            updatedAt = messageJson.optString("updatedAt", "unknown_date"),
                            encryptedAesKey = encryptedAesKey,
                            noty = messageJson.optBoolean("noty", false)
                        )

                        // Add the message to the adapter and scroll to the last position
                        chatAdapter.addMessageDecrypted(message)
                        Log.d("ChatDebug", "Message added to adapter: $message usernya sama dengan pengirim bos ku $senderId sama dengan $userId = ${senderId==userId}")
                        chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                        Log.d("ChatDebug", "Scrolled to latest message position.")
                    } else {
                        Log.e("ChatDebug", "Chat ID mismatch or essential data is missing, message not processed.")
                    }
                } catch (e: Exception) {
                    Log.e("ChatDebug", "Error processing message JSON: ${e.message}")
                    e.printStackTrace()
                }
            }
        }



        chatAdapter = ChatAdapter(emptyList(), userId ?: "", aesKey)
        chatRecyclerView.adapter = chatAdapter

        fetchMessages() // Fetch messages saat activity terbuka

        setupTypingListener(chatInput, chatId, userId)

        // Tambahkan ini di dalam onCreate() setelah inisialisasi view
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish() // Atau gunakan onBackPressed() untuk kembali ke activity sebelumnya
        }

    }

//    override fun onDestroy() {
//        super.onDestroy()
//        // Disconnect the socket connection when leaving the chat page
//        socketRepository.disconnect()
//        Log.d("ChatActivity", "Socket disconnected in onDestroy")
//    }


    private fun getUserIdFromSharedPrefs(): String? {
        val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        return sharedPref.getString("userId", null)
    }


    private fun setupTypingListener(chatInput: EditText, chatId: String?, userId: String?) {
        chatInput.addTextChangedListener {
            userId?.let { id ->
                chatId?.let { cid ->
                    socketRepository.sendTypingStatus(cid, id)
                }
            }
        }

        val typingTextView = findViewById<TextView>(R.id.typingStatusTextView)
        val typingHandler = android.os.Handler(Looper.getMainLooper())
        val typingRunnable = Runnable {
            typingTextView.text = ""
            typingTextView.visibility = View.GONE // Sembunyikan saat berhenti mengetik
        }

        socketRepository.receiveTypingStatus { typingJson ->
            runOnUiThread {
                val isTyping = typingJson.getBoolean("typing")
                val typingChatId = typingJson.getString("chatId") // Ambil chatId dari JSON
                val typingUserId = typingJson.getJSONObject("user").getString("_id")

                if (typingUserId != userId && chatId == typingChatId) {
                    if (isTyping) {
                        typingTextView.text = "is typing..."
                        typingTextView.visibility = View.VISIBLE // Tampilkan status typing
                        typingHandler.removeCallbacks(typingRunnable)
                        typingHandler.postDelayed(typingRunnable, 2000) // Hide after 2 seconds
                    } else {
                        typingTextView.text = ""
                        typingTextView.visibility = View.GONE // Sembunyikan jika tidak mengetik
                    }
                }
            }
        }
    }

    private fun getAuthToken(): String? {
        val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        return sharedPref.getString("authToken", null)
    }

    // Di dalam fetchMessages(), tambahkan scroll ke pesan terakhir
    private fun fetchMessages() {
        val chatId = intent.getStringExtra("CHAT_ID") ?: return
        val token = getAuthToken() ?: run {
            Log.e("ChatActivity", "Token tidak ditemukan.")
            return
        }

        lifecycleScope.launch {
            val result = ChatRepository(this@ChatActivity).fetchMessages(token, chatId)
            if (result.isSuccess) {
                val messages = result.getOrNull() ?: emptyList()
                messages.forEach { message ->
                    message.content = decryptMessageContent(message.content, message.encryptedAesKey, message.sender._id)
                }
                chatAdapter.updateMessages(messages)

                // Scroll ke pesan terakhir setelah pesan dimuat
                findViewById<RecyclerView>(R.id.chatRecyclerView).scrollToPosition(chatAdapter.itemCount - 1)
            } else {
                Log.e("ChatActivity", "Gagal memuat pesan: ${result.exceptionOrNull()}")
            }
        }
    }

    private fun loadAESKey() {
        val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val aesKeyString = sharedPref.getString("aesKey", null)
        if (aesKeyString != null) {
            aesKey = Base64.decode(aesKeyString, Base64.DEFAULT)
            Log.d("ChatActivity", "AES key found in SharedPreferences")
        } else {
            Log.e("ChatActivity", "AES key not found in SharedPreferences")
        }
    }
    private fun decryptMessageContent(
        encryptedContent: String,
        encryptedAesKey: String,
        senderId: String,
        isFetchMessage: Boolean = true
    ): String {
        val sharedPref = getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        val rsaPrivateKey = sharedPref.getString("rsaPrivateKey", null)
        val userId = intent.getStringExtra("USER_ID") ?: "defaultUserId"

        val currentTimestamp = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(currentTimestamp))

        Log.d("ChatActivity", "Decryption process started at: $formattedTime")
        Log.d("ChatActivity", "Starting decryption for message from sender: $senderId")
        Log.d("ChatActivity", "Encrypted content: $encryptedContent")
        Log.d("ChatActivity", "Encrypted AES key: $encryptedAesKey")

        val isSenderDifferentFromUser = if (isFetchMessage) senderId == userId else senderId != userId

        if (isSenderDifferentFromUser) {
            if (rsaPrivateKey != null) {
                Log.d("ChatActivity", "RSA private key found. Proceeding with decryption.")

                val startAesKeyDecryption = System.nanoTime() // Mulai timing dekripsi AES key
                val encryptedAesKeyArray = encryptedAesKey.split(",").map { it.toLong() }.toLongArray()
                Log.d("ChatActivity", "Converted encrypted AES key to LongArray: ${encryptedAesKeyArray.joinToString(", ")}")

                val aesKeyDecryptedString = RSA.decrypt(encryptedAesKeyArray, rsaPrivateKey)
                val endAesKeyDecryption = System.nanoTime() // Selesai timing dekripsi AES key
                val aesKeyDecryptionDuration = (endAesKeyDecryption - startAesKeyDecryption) / 1_000_000 // Dalam ms

                Log.d("DecryptionTime AES Key", "AES key decryption time: $aesKeyDecryptionDuration ms")
                Log.d("ChatActivity", "Decrypted AES key string: $aesKeyDecryptedString")

                val aesKeyDecrypted = Base64.decode(aesKeyDecryptedString, Base64.DEFAULT)
                Log.d("ChatActivity", "Decoded decrypted AES key (byte array): ${aesKeyDecrypted.joinToString(", ") { it.toString() }}")

                return try {
                    val startMessageDecryption = System.nanoTime() // Mulai timing dekripsi pesan
                    val decryptedContent = AES.decrypt(aesKeyDecrypted, Base64.decode(encryptedContent, Base64.DEFAULT))
                    val endMessageDecryption = System.nanoTime() // Selesai timing dekripsi pesan
                    val messageDecryptionDuration = (endMessageDecryption - startMessageDecryption) / 1_000_000 // Dalam ms

                    Log.d("DecryptionTime Message", "Message decryption time: $messageDecryptionDuration ms")
                    decryptedContent
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Failed to decrypt message at $formattedTime: ${e.message}", e)
                    "[Encrypted Message]"
                }
            } else {
                Log.e("ChatActivity", "RSA private key not found at $formattedTime.")
                return "[Encrypted Message]"
            }
        } else {
            Log.d("ChatActivity", "Message from self, no decryption needed. Timestamp: $formattedTime")
            return try {
                val startSelfMessageDecryption = System.nanoTime() // Mulai timing dekripsi pesan sendiri
                val decryptedContent = AES.decrypt(aesKey, Base64.decode(encryptedContent, Base64.DEFAULT))
                val endSelfMessageDecryption = System.nanoTime() // Selesai timing dekripsi pesan sendiri
                val selfMessageDecryptionDuration = (endSelfMessageDecryption - startSelfMessageDecryption) / 1_000_000 // Dalam ms

                Log.d("DecryptionTime Self Message", "Self message decryption time: $selfMessageDecryptionDuration ms")
                decryptedContent
            } catch (e: Exception) {
                Log.e("ChatActivity", "Failed to decrypt self message at $formattedTime: ${e.message}", e)
                "[Encrypted Message]"
            }
        }
    }

    private fun encryptMessage(message: String): ByteArray {
        val startTime = System.nanoTime() // Waktu mulai
        val encryptedMessage = AES.encrypt(aesKey, message)
        val endTime = System.nanoTime() // Waktu selesai

        val duration = (endTime - startTime) / 1_000_000.0 // Durasi dalam milidetik
        Log.d("EncryptionTime", "Message encryption time: $duration ms")

        return encryptedMessage
    }

    private fun decryptMessage(encryptedMessage: ByteArray) {
        try {
            val decryptedMessage = AES.decrypt(aesKey, encryptedMessage)
            Log.d("ChatActivity", "Decrypted message: $decryptedMessage")
            // Debug: Cetak nilai aesKey sebagai string heksadesimal
            Log.d("ChatActivity", "AES Key dalam bytearray: $aesKey")
            val encodAes=Base64.encodeToString(aesKey, Base64.DEFAULT)
            Log.d("ChatActivity", "Encoding base64 AES Key: $encodAes")
        } catch (e: Exception) {
            Log.e("ChatActivity", "Decryption failed: ${e.message}")
        }
    }

    private fun sendMessage(encryptedMessage: ByteArray, encryptedAesKeyString: String) {
        val token = getAuthToken() ?: run {
            Log.e("ChatActivity", "Token not found.")
            return
        }

        val chatId = intent.getStringExtra("CHAT_ID") ?: run {
            Log.e("ChatActivity", "Chat ID not found.")
            return
        }

        val encryptedMessageString = Base64.encodeToString(encryptedMessage, Base64.DEFAULT)

        val userId = intent.getStringExtra("USER_ID") ?: return

        lifecycleScope.launch {
            try {
                val result = ChatRepository(this@ChatActivity).sendMessage(token, encryptedMessageString, chatId, encryptedAesKeyString)
                if (result.isSuccess) {
                    val sentMessageRoom = result.getOrNull()
                    if (sentMessageRoom != null) {
                        Log.d("ChatActivity", "Message sent: ${sentMessageRoom.content}")
                        chatAdapter.addMessage(sentMessageRoom.toMessage())
                        findViewById<RecyclerView>(R.id.chatRecyclerView).scrollToPosition(chatAdapter.itemCount - 1)

                        // Kirim pesan ke server lewat SocketRepository setelah berhasil
                        socketRepository.sendMessage(chatId, encryptedMessageString, encryptedAesKeyString, userId)
                    }
                } else {
                    Log.e("ChatActivity", "Failed to send message: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Exception during sendMessage: ${e.message}")
            }
        }
    }

}