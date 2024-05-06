package com.example.chaqmoq.ui.targetUser

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chaqmoq.model.Message
import com.example.chaqmoq.network.MyHTTPClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import org.json.JSONException

class TargetUserViewModel : ViewModel() {
    private val myHttpClient = MyHTTPClient()
    private val _messageList = MutableLiveData<List<Message>>()
    val messageList: LiveData<List<Message>> get() = _messageList

    val editTextValue = MutableLiveData<String>()

    fun onEditTextChanged(newValue: String) {
        editTextValue.value = newValue
    }

    init {
        makeNetworkRequest()
    }

    fun makeNetworkRequest() {
        val url = "http://192.168.41.168:5000/messages"
        val message = "027aaaaddeeffgghhhhiinnoorrvv"
        val requestBody = "{\"message\": \"$message\"}"
        viewModelScope.launch {
            try {
                val response = myHttpClient.postRequest(url, requestBody)
                val usersMap: Map<String, Message> = Gson().fromJson(response, object : TypeToken<Map<String, Message>>() {}.type)
                val userList: List<Message> = usersMap.values.toList()
                _messageList.postValue(userList)
                Log.i("list",   "list of $response")
            } catch (e: JSONException) {
                Log.e("MyTag", "JSON Error: ${e.message}")
                // Handle JSON parsing error here
            } catch (e: Exception) {
                Log.e("MyTag", "Error: ${e.message}")
                // Handle other errors here
            }
        }
    }
}