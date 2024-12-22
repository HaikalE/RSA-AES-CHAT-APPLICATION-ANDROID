package com.example.imrsaaes.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imrsaaes.ChatActivity
import com.example.imrsaaes.R
import com.example.imrsaaes.cryptography.AES
import com.example.imrsaaes.cryptography.RSA
import com.example.imrsaaes.model.Chat
import com.example.imrsaaes.repository.ChatRepository
import com.example.imrsaaes.repository.SocketRepository
import com.example.imrsaaes.util.ChatListAdapter
import kotlinx.coroutines.launch

class ChatListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatRepository : ChatRepository
    private var chatList: List<Chat> = listOf() // Simpan daftar chat untuk referensi nanti
    private lateinit var socketRepository: SocketRepository // Instance SocketRepository
    private var userId: String? = null // Declare userId here

    override fun onResume() {
        super.onResume()
        // Cek apakah userId sudah ada, jika belum setup ulang
        socketRepository = SocketRepository()
        userId?.let {
            socketRepository.setupUser(null, it) // Setup jika userId tersedia
        } ?: run {
            Log.w("ChatListFragment", "User ID not found, setupUser skipped.")
        }
        loadChats() // Panggil lagi loadChats() buat update daftar chat
        startListeningForMessages() // Start listening for new messages
    }

    private fun startListeningForMessages() {
        socketRepository.listenForMessages {
            if (isAdded) {  // Pastikan fragment masih terhubung ke activity
                requireActivity().runOnUiThread {
                    try {
                        Log.d("ChatListFragment", "New message received, reloading chat list")
                        loadChats() // Reload the chat list whenever a message is received
                    } catch (e: Exception) {
                        Log.e("ChatListFragment", "Error processing incoming message: ${e.message}")
                        e.printStackTrace()
                    }
                }
            } else {
                Log.w("ChatListFragment", "Fragment not attached, skipping message handling.")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)
        recyclerView = view.findViewById(R.id.recyclerChatList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inisialisasi chatRepository di sini
        chatRepository = ChatRepository(requireContext())
        loadChats()
        socketRepository = SocketRepository()
        // Initialize userId and pass it to setupUser
        userId = getUserIdFromSharedPrefs()
        userId?.let { id -> socketRepository.setupUser(null, id) }
        return view
    }

    private fun getUserIdFromSharedPrefs(): String? {
        val sharedPref = requireActivity().getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        return sharedPref.getString("userId", null)
    }

    private fun getEmailFromPreferences(): String? {
        val sharedPref = requireActivity().getSharedPreferences("MyAppPreferences", android.content.Context.MODE_PRIVATE)
        return sharedPref.getString("email", null)
    }

    override fun onPause() {
        super.onPause()
        socketRepository.disconnect() // Matikan koneksi socket saat fragment tidak aktif
        Log.d("ChatListFragment", "Socket disconnected onPause")
    }

    private fun loadChats() {
        val authToken = getAuthToken()
        val storedEmail = getEmailFromPreferences()

        if (authToken != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = chatRepository.fetchRecentChats(authToken)
                result.onSuccess { chats ->
                    Log.d("ChatListFragment", "Fetched chats: $chats")

                    chatList = chats.map { chat ->
                        Log.d("ChatListFragment", "Initial latestMessage content: ${chat.latestMessage?.content}")

                        val latestMsgContent = chat.latestMessage?.content ?: "Tidak ada pesan"
                        val senderEmail = chat.latestMessage?.sender?.email

                        Log.d("ChatListFragment", "Sender email: $senderEmail, Stored email: $storedEmail")

                        val decryptedContent = if (senderEmail == storedEmail) {
                            Log.d("ChatListFragment", "Using personal AES decryption for content: $latestMsgContent")
                            decryptUsingPersonalAes(latestMsgContent)
                        } else {
                            Log.d("ChatListFragment", "Using chat decryption with encrypted key: ${chat.latestMessage?.encryptedAesKey}")
                            decryptChatContent(latestMsgContent, chat.latestMessage?.encryptedAesKey)
                        }

                        chat.latestMessage?.content = decryptedContent
                        Log.d("ChatListFragment", "Decrypted content: $decryptedContent")

                        chat
                    }

                    Log.d("ChatListFragment", "Updated chat list with decrypted messages: $chatList")

                    val userEmail = getEmailFromPreferences()
                    val adapter = ChatListAdapter(requireContext(), chatList, userEmail ?: "") { chat ->
                        if (userEmail != null) {
                            openChatActivity(chat)
                        }
                    }
                    recyclerView.adapter = adapter
                }.onFailure { error ->
                    Log.e("ChatListFragment", "Gagal memuat chat", error)
                }
            }
        } else {
            Log.w("ChatListFragment", "Auth token tidak ditemukan, arahkan ke login.")
        }
    }

    private fun openChatActivity(chat: Chat) {
        val userEmail = getEmailFromPreferences()
        val authToken = getAuthToken()

        if (authToken == null) {
            Log.e("ChatListFragment", "Auth token tidak ditemukan.")
            return
        }

        val otherUser = chat.users.firstOrNull { it.user.email != userEmail }?.user

        if (otherUser != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                val response = chatRepository.accessChat(authToken, otherUser._id)
                response.onSuccess { accessedChat ->
                    val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                        putExtra("CHAT_ID", accessedChat._id)
                        putExtra("CHAT_NAME", accessedChat.chatName ?: "Chat with ${otherUser.name}")
                        putExtra("USER_NAME", otherUser.name ?: "Name not found")
                        putExtra("USER_ID", otherUser._id)
                        putExtra("RSA_PUBLIC_KEY", otherUser.rsaPublic)
                    }
                    startActivity(intent)
                }.onFailure { error ->
                    Log.e("ChatListFragment", "Gagal mengakses chat: ${error.message}")
                }
            }
        } else {
            Log.e("ChatListFragment", "No other user found in the chat.")
        }
    }

    private fun decryptUsingPersonalAes(encryptedContent: String): String {
        val sharedPref = requireActivity().getSharedPreferences("MyAppPreferences", android.content.Context.MODE_PRIVATE)
        val aesKeyBase64 = sharedPref.getString("aesKey", null)
        if (aesKeyBase64.isNullOrEmpty()) {
            Log.e("ChatListFragment", "AES key pribadi tidak ditemukan.")
            return "[Encrypted Message]"
        }

        return try {
            val aesKey = Base64.decode(aesKeyBase64, Base64.DEFAULT)
            AES.decrypt(aesKey, Base64.decode(encryptedContent, Base64.DEFAULT))
        } catch (e: Exception) {
            Log.e("ChatListFragment", "Gagal mendekripsi konten pesan dengan AES pribadi: ${e.message}")
            "[Encrypted Message]"
        }
    }

    private fun decryptChatContent(encryptedContent: String, encryptedAesKey: String?): String {
        val sharedPref = requireActivity().getSharedPreferences("MyAppPreferences", android.content.Context.MODE_PRIVATE)
        val rsaPrivateKey = sharedPref.getString("rsaPrivateKey", null)
        Log.d("ChatListFragment", "Encrypted AES key = $encryptedAesKey dan RSA PRIVATE = $rsaPrivateKey")
        if (encryptedAesKey.isNullOrEmpty() || rsaPrivateKey.isNullOrEmpty()) {
            Log.e("ChatListFragment", "Either encrypted AES key or RSA private key is missing.")
            return "[Encrypted Message]"
        }

        return try {
            val encryptedAesKeyArray = encryptedAesKey.split(",").map { it.toLong() }.toLongArray()
            Log.d("ChatListFragment", "Converted encrypted AES key to LongArray: ${encryptedAesKeyArray.joinToString(", ")}")
            val aesKeyDecryptedString = RSA.decrypt(encryptedAesKeyArray, rsaPrivateKey)
            val aesKeyDecrypted = Base64.decode(aesKeyDecryptedString, Base64.DEFAULT)
            AES.decrypt(aesKeyDecrypted, Base64.decode(encryptedContent, Base64.DEFAULT))
        } catch (e: Exception) {
            Log.e("ChatListFragment", "Failed to decrypt message content: ${e.message}")
            "[Encrypted Message]"
        }
    }

    private fun getAuthToken(): String? {
        val sharedPref = requireActivity().getSharedPreferences("MyAppPreferences", MODE_PRIVATE)
        return sharedPref.getString("authToken", null)
    }
}
