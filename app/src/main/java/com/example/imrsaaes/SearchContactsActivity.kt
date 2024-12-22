package com.example.imrsaaes


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imrsaaes.cryptography.AES
import com.example.imrsaaes.model.Chat
import com.example.imrsaaes.repository.ChatRepository
import com.example.imrsaaes.repository.UserRepository
import kotlinx.coroutines.launch

class SearchContactsActivity : AppCompatActivity() {

    private lateinit var userRepository: UserRepository
    private lateinit var adapter: UserAdapter
    private lateinit var searchInput: EditText
    private lateinit var chatRepository: ChatRepository // Inisialisasi instance ChatRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_contacts)

        val authToken = intent.getStringExtra("AUTH_TOKEN")  // Ambil token dari intent
        chatRepository = ChatRepository(this@SearchContactsActivity) // Inisialisasi ChatRepository langsung di sini


        // Inisialisasi UserRepository dan ChatRepository
        userRepository = UserRepository(this@SearchContactsActivity)


        // In UserAdapter onClick
        adapter = UserAdapter { selectedUser ->
            if (authToken != null) {
                accessChatWithUser(selectedUser._id, authToken, selectedUser.rsaPublic) // Pass rsaPublic key here
            } else {
                Toast.makeText(this, "Token tidak tersedia!", Toast.LENGTH_SHORT).show()
            }
        }

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = adapter

        // Setup Search Input
        searchInput = findViewById(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    if (authToken != null) {
                        searchUsers(s.toString(),authToken)
                    }
                }
            }
        })

        testAesEncryptionDecryption()
    }

    private fun testAesEncryptionDecryption() {
        Log.d("AES", "=== Testing AES Encryption and Decryption ===")

        // Hardcoded AES Key
        val aesKey = byteArrayOf(
            111, -94, 27, 111, 122, -112, 1, -67, 91, 94, 12, -127, -104, 27, 95, 6,
            22, 6, 12, 18, -120, -28, -64, 39, 118, 103, 91, -96, -126, -97, -84, -125
        )
        Log.d("AES", "Using predefined AES Key: ${aesKey.joinToString("") { "%02x".format(it) }}")

        // Define plaintext to encrypt
        val plaintext = "Kriptografi 2024"
        Log.d("AES", "Plaintext: $plaintext")

        try {
            // Encrypt the plaintext
//            val encryptedData = AES.encrypt(aesKey, plaintext)
//            Log.d("AES", "Encrypted Data (Hex): ${encryptedData.joinToString("") { "%02x".format(it) }}")

            val decryptedText = AES.decrypt(aesKey, hexStringToByteArray("ac4048912fb4fa7173037f69f4802eb7"))
            Log.d("AES", "Decrypted Text: $decryptedText")

            // Verify if the decrypted text matches the original plaintext
            if (plaintext.trim() == decryptedText.trim()) {
                Log.d("AES", "Test Passed: Decrypted text matches the original plaintext!")
            } else {
                Log.d("AES", "Test Failed: Decrypted text does not match the original plaintext.")
            }

            // Decrypt the encrypted data
//            val decryptedText = AES.decrypt(aesKey, encryptedData)
//            Log.d("AES", "Decrypted Text: $decryptedText")
//
//            // Verify if the decrypted text matches the original plaintext
//            if (plaintext.trim() == decryptedText.trim()) {
//                Log.d("AES", "Test Passed: Decrypted text matches the original plaintext!")
//            } else {
//                Log.d("AES", "Test Failed: Decrypted text does not match the original plaintext.")
//            }
        } catch (e: Exception) {
            Log.e("AES", "Error during AES encryption/decryption: ${e.message}", e)
        }
    }

    fun hexStringToByteArray(hex: String): ByteArray {
        val length = hex.length
        val byteArray = ByteArray(length / 2)
        for (i in hex.indices step 2) {
            val byte = hex.substring(i, i + 2).toInt(16)
            byteArray[i / 2] = byte.toByte()
        }
        return byteArray
    }

    // Modify the accessChatWithUser function to receive rsaPublic
    private fun accessChatWithUser(userTwoId: String, authToken: String, rsaPublic: String) {
        lifecycleScope.launch {
            Log.d("accessChatWithUser", "Starting accessChat for userTwoId: $userTwoId with authToken: $authToken and RSA Public Key: $rsaPublic")

            // Call chatRepository to access chat
            val response = chatRepository.accessChat(authToken, userTwoId)
            response.onSuccess { chat ->
                Log.d("accessChatWithUser", "accessChat success: Received Chat ID = ${chat._id}, Chat Name = ${chat.chatName}")
                val myUserId = getMyUserId(chat, this@SearchContactsActivity)
                val chatName = chat.chatName ?: "Chat with ${myUserId?.let {
                    findChatPartnerName(chat,
                        it
                    )
                }}"
                val partnerName = myUserId?.let { findChatPartnerName(chat, it) }

                // Retrieve current user ID for resetting unseen messages

                if (myUserId != null) {
                    Log.d("accessChatWithUser", "Calling chatRepository.getUnseenMessageCount with myUserId = $myUserId")

                    // Fetch unseen message count
                    val unseenCountResponse = chatRepository.getUnseenMessageCount(authToken, chat._id, myUserId)
                    unseenCountResponse.onSuccess { unseenCount ->
                        Log.d("accessChatWithUser", "getUnseenMessageCount success: Unseen Message Count = $unseenCount")

                        // Reset unseen message count
                        val resetUnseenResponse = chatRepository.getUnseenMessageCount(authToken, chat._id, myUserId)
                        resetUnseenResponse.onSuccess {
                            Log.d("accessChatWithUser", "Unseen message count reset successfully for Chat ID = ${chat._id}")

                            // Start ChatActivity
                            val intent = Intent(this@SearchContactsActivity, ChatActivity::class.java).apply {
                                putExtra("CHAT_ID", chat._id)
                                putExtra("CHAT_NAME", chatName)
                                putExtra("USER_NAME", partnerName)
                                putExtra("USER_ID", userTwoId)
                                putExtra("RSA_PUBLIC_KEY", rsaPublic)
                            }
                            startActivity(intent)
                        }.onFailure { exception ->
                            Log.e("accessChatWithUser", "Error resetting unseen messages: ${exception.message}")
                            Toast.makeText(this@SearchContactsActivity, "Error resetting unseen messages: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { exception ->
                        Log.e("accessChatWithUser", "Error fetching unseen messages: ${exception.message}")
                        Toast.makeText(this@SearchContactsActivity, "Error fetching unseen messages: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("accessChatWithUser", "Current user ID not found.")
                }
            }.onFailure { exception ->
                Log.e("accessChatWithUser", "Error accessing chat: ${exception.message}")
                Toast.makeText(this@SearchContactsActivity, "Error accessing chat: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun getMyUserId(chat: Chat, context: Context): String? {
        // Ambil email dari SharedPreferences
        val sharedPref = context.getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val myEmail = sharedPref.getString("email", null) // Ambil email yang disimpan

        // Jika email ditemukan di SharedPreferences, cari user ID yang sesuai
        return if (myEmail != null) {
            chat.users.find { userWrapper ->
                userWrapper.user.email == myEmail
            }?.user?._id
        } else {
            null // Jika email tidak ditemukan, kembalikan null
        }
    }




    private fun findChatPartnerName(chat: Chat, currentUserId: String): String {
        // Log objek chat secara keseluruhan untuk memeriksa struktur data
        Log.d("FindChatPartnerName", "Raw Chat Object: $chat")

        // Log untuk memastikan ID pengguna yang dicari
        Log.d("FindChatPartnerName", "Current User ID: $currentUserId")

        // Temukan partner chat yang bukan pengguna saat ini
        val partner = chat.users.find { userWrapper ->
            userWrapper.user._id != currentUserId
        }

        // Cek hasil pencarian dan log hasilnya
        val partnerName = partner?.user?.name ?: "Unknown"
        Log.d("FindChatPartnerName", "Found partner name: $partnerName for user ID: $currentUserId")

        return partnerName
    }






    private fun searchUsers(query: String,token: String) {
        if (token == null) {
            Toast.makeText(this, "Token tidak tersedia!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = userRepository.searchUsers(query, token)
            result.onSuccess { users ->
                adapter.submitList(users)
            }.onFailure { exception ->
                Toast.makeText(this@SearchContactsActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
