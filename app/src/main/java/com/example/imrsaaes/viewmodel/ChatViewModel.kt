package com.example.imrsaaes.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imrsaaes.model.Chat
import com.example.imrsaaes.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    fun getAccessChat(token: String, userTwo: String): LiveData<Result<Chat>> {
        val liveData = MutableLiveData<Result<Chat>>()

        viewModelScope.launch {
            val result = chatRepository.accessChat(token, userTwo)
            liveData.postValue(result)
        }

        return liveData
    }
}
