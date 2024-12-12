package com.example.chaqmoq.ui.home

import android.content.ContentProvider
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.ViewModelProvider
import com.example.chaqmoq.model.User
import com.example.chaqmoq.network.MyHTTPClient
import com.example.chaqmoq.repos.SocketRepository
import kotlinx.coroutines.launch
import org.json.JSONException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class HomeViewModel : ViewModel() {
    private val myHttpClient = MyHTTPClient()
    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>> get() = _userList
    init {
        makeNetworkRequest()
    }

    fun makeNetworkRequest() {
        val ip = SocketRepository.ip
        viewModelScope.launch {
            try {
                val response = myHttpClient.getRequest("${ip}/userlist")
                Log.d("response", response)
                val usersMap: Map<String, User> = Gson().fromJson(response, object : TypeToken<Map<String, User>>() {}.type)
                Log.d("userMap", usersMap.toString())
                val userList: List<User> = usersMap.values.toList()
                _userList.postValue(userList)
                Log.i("list",   "list: $userList")
            } catch (e: JSONException) {
                Log.e("messages", "JSON Error: ${e.message}")
            } catch (e: Exception) {
                Log.e("messages", "Error: ${e.message}")
            }
        }
    }
}