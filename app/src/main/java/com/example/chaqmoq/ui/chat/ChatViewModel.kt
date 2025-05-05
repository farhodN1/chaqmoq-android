package com.example.chaqmoq.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.chaqmoq.model.User
import com.example.chaqmoq.network.MyHTTPClient
import com.example.chaqmoq.repos.DatabaseRepository.getAllUsers
import kotlinx.coroutines.launch
import org.json.JSONException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers


class ChatViewModel : ViewModel() {
    private val myHttpClient = MyHTTPClient()
    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>> get() = _userList

    fun fetchUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = myHttpClient.getRequest("/userlist")
                val usersMap: Map<String, User> = Gson().fromJson(response, object : TypeToken<Map<String, User>>() {}.type)
                val userList: List<User> = usersMap.values.toList()
                _userList.postValue(userList)
            } catch (e: JSONException) {
                _userList.postValue(getAllUsers())
                Log.e("log", "JSON Error: ${e.message}")
            } catch (e: Exception) {
                _userList.postValue(getAllUsers())
                Log.e("log", "Error: ${e.message}")
            }
        }
    }
}