package com.example.imrsaaes.util

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imrsaaes.R
import com.example.imrsaaes.model.Chat

class ChatListAdapter(
    private val context: Context,
    private val chatList: List<Chat>,
    private val userEmail: String,  // Tambahkan email pengguna sebagai parameter
    private val onItemClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.userName)
        val latestMessage: TextView = view.findViewById(R.id.latestMessage)
        val unseenMessageCount: TextView = view.findViewById(R.id.unseenMessageCount)

        fun bind(chat: Chat) {
            // Ambil nama pengguna lain selain pengguna saat ini
            val otherUserName = chat.users
                .firstOrNull { it.user.email != userEmail }
                ?.user?.name ?: "Nama tidak ditemukan"

            Log.d("ChatListAdapter", "Pengguna lain dalam chat: $otherUserName")
            userName.text = otherUserName

            // Debugging untuk melihat email sender dari pesan terbaru
            val senderEmail = chat.latestMessage?.sender?.email
            Log.d("ChatListAdapter", "Sender email dari latestMessage: $senderEmail")

            // Cek apakah pengirim adalah pengguna saat ini
            val isSentByUser = senderEmail == userEmail
            val latestMessageContent = chat.latestMessage?.content ?: "Tidak ada pesan"
            Log.d("ChatListAdapter", "Apakah pesan terbaru dikirim oleh pengguna? $isSentByUser")
            Log.d("ChatListAdapter", "Konten pesan terbaru: $latestMessageContent")

            // Tampilkan "You" jika pengirim adalah pengguna, atau tampilkan langsung jika bukan
            latestMessage.text = if (isSentByUser) {
                "You: $latestMessageContent"
            } else {
                latestMessageContent
            }
            Log.d("ChatListAdapter", "Isi latestMessage yang ditampilkan: ${latestMessage.text}")

            // Set unseen message count khusus untuk pengguna saat ini
            val userUnseenMessages = chat.users
                .firstOrNull { it.user.email == userEmail }
                ?.unseenMsg ?: 0
            Log.d("ChatListAdapter", "Jumlah pesan belum terbaca untuk pengguna saat ini: $userUnseenMessages")

            // Tampilkan atau sembunyikan jumlah pesan belum terbaca
            if (userUnseenMessages > 0) {
                unseenMessageCount.visibility = View.VISIBLE
                unseenMessageCount.text = userUnseenMessages.toString()
                Log.d("ChatListAdapter", "Pesan belum terbaca ditampilkan: $userUnseenMessages")
            } else {
                unseenMessageCount.visibility = View.GONE
                Log.d("ChatListAdapter", "Tidak ada pesan belum terbaca")
            }

            itemView.setOnClickListener {
                Log.d("ChatListAdapter", "Item chat diklik: ${chat._id}")
                onItemClick(chat)
            }
        }


    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    override fun getItemCount() = chatList.size
}
