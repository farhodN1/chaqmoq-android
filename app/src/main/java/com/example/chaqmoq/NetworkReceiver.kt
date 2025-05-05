package com.example.chaqmoq

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import com.example.chaqmoq.db.MessageListDatabase.Companion.isDatabaseInit
import com.example.chaqmoq.model.Message
import com.example.chaqmoq.repos.DatabaseRepository.getUnsentMessage
import com.example.chaqmoq.repos.DatabaseRepository.saveMessage
import com.example.chaqmoq.repos.SocketRepository
import com.example.chaqmoq.utils.Firebase.uploadFileToFirestore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.UUID

class NetworkReceiver : BroadcastReceiver() {
    init {
        Log.d("networkReceiver", "initialized")
    }
    override fun onReceive(context: Context, intent: Intent) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected
        // Do something with isConnected
        Log.d("isInternetAvailable", isConnected.toString())
        if (isConnected) {
            val thread = Thread {
                runBlocking {
                    if (isDatabaseInit()) {
                        val unsent_messages = getUnsentMessage()
                        for (message in unsent_messages) {
                            Log.d("message", message.status.toString())
                            sendToServer(message)
                            message.status = "sent"
                            saveMessage(message)
                        }
                    }
                }
            }
            thread.start()
        }
    }
    suspend fun sendToServer(message: Message) {
        val jsonObject = JSONObject()
        jsonObject.put("id", UUID.randomUUID().toString())
        jsonObject.put("sender", message.sender_id)
        jsonObject.put("recipient", message.receiver_id)
        jsonObject.put("sendTime", message.send_time)
        if (message.message_type == "audio") {
            val pathString = "VoiceMessages/voice_message_${System.currentTimeMillis()}.m4a"
            val cloudPath = uploadFileToFirestore(Uri.parse(("file://" + message.message)), pathString)
            Log.d("cloud path", cloudPath!!)
            jsonObject.put("message", message.message)
            jsonObject.put("message_type", message.message_type)
            jsonObject.put("amplitudes", message.amplitudes)
        } else {
            jsonObject.put("message", message.message)
            jsonObject.put("message_type", message.message_type)
        }
        SocketRepository.socket.emit("private message", jsonObject)
    }

}
