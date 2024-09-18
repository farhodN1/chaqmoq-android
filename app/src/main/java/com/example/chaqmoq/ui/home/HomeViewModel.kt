package com.example.chaqmoq.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.chaqmoq.model.User
import com.example.chaqmoq.network.MyHTTPClient
import kotlinx.coroutines.launch
import org.json.JSONException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class HomeViewModel : ViewModel() {
    private val myHttpClient = MyHTTPClient()
    private val _userList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>> get() = _userList

    private val _text = MutableLiveData<String>().apply {
        value = "This is gallery Fragment"
    }
    val text: LiveData<String> = _text

    init {
        makeNetworkRequest()
    }


    fun makeNetworkRequest() {
        viewModelScope.launch {
            try {
                val response = myHttpClient.getRequest("http://192.168.222.115:5000/userlist")
                val usersMap: Map<String, User> = Gson().fromJson(response, object : TypeToken<Map<String, User>>() {}.type)
                val userList: List<User> = usersMap.values.toList()
                _userList.postValue(userList)
                Log.i("list",   "list: $userList")
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