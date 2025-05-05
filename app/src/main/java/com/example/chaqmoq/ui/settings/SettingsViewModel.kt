package com.example.chaqmoq.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaqmoq.repos.DatabaseRepository.deleteAllConversations
import com.example.chaqmoq.repos.DatabaseRepository.deleteUsers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsViewModel: ViewModel() {

    fun clearConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            deleteAllConversations()
        }
    }

    fun clearUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            deleteUsers()
        }
    }
}