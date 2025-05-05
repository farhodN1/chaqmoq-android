package com.example.chaqmoq.ui.targetUser

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaqmoq.model.Message
import com.example.chaqmoq.network.MyHTTPClient
import com.example.chaqmoq.repos.DatabaseRepository.getConversationByConId
import com.example.chaqmoq.repos.SocketRepository
import com.example.chaqmoq.repos.WebRTCRepository
import com.example.news.utils.NetworkUtils.isInternetAvailable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException

class TargetUserViewModel : ViewModel() {
    private val myHttpClient = MyHTTPClient()
    private val _messageList = MutableLiveData<List<Message>>()
    val messageList: LiveData<List<Message>> get() = _messageList

    fun fetchMessages(conId: String, context: Context) {
        val requestBody = "{\"conId\": \"$conId\"}"
        if (isInternetAvailable(context)) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val response = myHttpClient.postRequest("messages", requestBody)
                    val usersMap: Map<String, Message> = Gson().fromJson(response, object : TypeToken<Map<String, Message>>() {}.type)
                    val userList: List<Message> = usersMap.values.toList()
                    _messageList.postValue(userList)
                } catch (e: JSONException) {
                    Log.e("Error", "JSON Error: ${e.message}")
                    _messageList.postValue(getConversationByConId(conId))
                } catch (e: Exception) {
                    Log.e("Error", "Error: ${e.message}")
                    _messageList.postValue(getConversationByConId(conId))
                }
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                Log.d("log", "fetching messages")
                _messageList.postValue(getConversationByConId(conId))
            }
        }

    }
}