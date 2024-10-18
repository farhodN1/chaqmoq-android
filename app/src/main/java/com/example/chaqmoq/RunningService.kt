package com.example.chaqmoq

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.chaqmoq.repos.SocketRepository

class RunningService: Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("test", "phase 1")
        SocketRepository.socket.on("call") { msg -> Log.d("call", "incoming call ${msg[0]}")}
        return START_STICKY
    }
}

