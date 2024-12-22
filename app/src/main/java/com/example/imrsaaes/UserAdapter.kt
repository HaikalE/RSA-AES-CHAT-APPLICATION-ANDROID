package com.example.imrsaaes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imrsaaes.model.SearchUser

class UserAdapter(private val onItemClick: (SearchUser) -> Unit) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var userList: List<SearchUser> = emptyList()

    fun submitList(users: List<SearchUser>) {
        userList = users
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount() = userList.size

    class UserViewHolder(itemView: View, private val onItemClick: (SearchUser) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.userName)

        fun bind(user: SearchUser) {
            nameTextView.text = user.name
            itemView.setOnClickListener {
                onItemClick(user)
            }
        }
    }
}
