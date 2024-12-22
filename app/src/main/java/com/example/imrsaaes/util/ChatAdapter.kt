package com.example.imrsaaes.util

import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imrsaaes.R
import com.example.imrsaaes.cryptography.AES
import com.example.imrsaaes.model.Message

class ChatAdapter(
    private var messages: List<Message>,
    private val userId: String,
    private val aesKey: ByteArray // Tambahkan kunci AES jika diperlukan untuk dekripsi
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        val senderId = messages[position].sender._id
        val idMessage = messages[position]._id
        return if (senderId != userId && idMessage != "unknown_id") VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }


    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageContent: TextView = itemView.findViewById(R.id.messageContent)

        fun bind(message: Message) {
            // Konten pesan yang sudah didekripsi langsung ditampilkan
            messageContent.text = message.content
        }
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    fun addMessage(newMessage: Message) {
        // Dekripsi konten pesan baru jika perlu
        try {
            val decodedContent = Base64.decode(newMessage.content, Base64.DEFAULT)
            val decryptedContent = AES.decrypt(aesKey, decodedContent) // Menggunakan kunci AES untuk dekripsi
            newMessage.content = decryptedContent // Update konten pesan dengan teks terdekripsi
        } catch (e: Exception) {
            Log.e("ChatAdapter", "Failed to decrypt new message: ${e.message}", e)
            newMessage.content = "[Encrypted Message]"
        }

        // Tambahkan pesan baru ke daftar dan beri tahu adapter
        messages = messages + newMessage
        notifyItemInserted(messages.size - 1)
    }

    fun addMessageDecrypted(newMessage: Message) {
        // Cek apakah ID pengirim pesan sama dengan userId saat ini
        val viewType = if (newMessage.sender._id == userId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
        Log.d("ChatAdapter", "VIEW TYPE PESAN ENTE SEBAGAI sent ==${newMessage.sender._id == userId}")
        // Tambahkan pesan baru ke daftar pesan
        messages = messages + newMessage

        // Beri tahu adapter tentang perubahan posisi
        notifyItemInserted(messages.size - 1)
    }


}
