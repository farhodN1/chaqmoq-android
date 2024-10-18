package com.example.chaqmoq

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.chaqmoq.repos.SocketRepository
import org.json.JSONObject

class CallService : Service() {
    private var incomingCallAlert: IncomingCallAlert? = null

    override fun onCreate() {
        super.onCreate()
        incomingCallAlert = IncomingCallAlert()
        createNotificationChannel()
        SocketRepository.socket.on("audioCall") {msg ->
            if (msg.isNotEmpty()) {
                Log.d("farhod", "${ msg[0] }")
                val data = JSONObject(msg[0].toString())
                val sender = data.optString("sender")
                val image = data.optString("image")
                Log.d("socket", "audioCall ${sender}")
                SocketRepository.onSocketConnection()
                runOnMainThread {
                    incomingCallAlert?.showWindow(this, sender, image, "audio call")
                }
            } else {
                Log.e("socket", "audioCall message array is empty")
            }
            SocketRepository.onSocketConnection()
        }
        SocketRepository.socket.on("videoCall") {msg ->
            if (msg.isNotEmpty()) {
                val data = JSONObject(msg[0].toString())
                val sender = data.optString("sender")
                val image = data.optString("image")
                Log.d("socket", "call ${sender}")
                SocketRepository.onSocketConnection()
                runOnMainThread{
                    incomingCallAlert?.showWindow(this, sender, image, "video call")
                }

            } else {
                Log.e("socket", "videoCall message array is empty")
            }
            SocketRepository.onSocketConnection()
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        val mainHandler = android.os.Handler(mainLooper)
        mainHandler.post { action() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) : Int {
        val nickname = intent?.getStringExtra("username")
        Log.d("id", nickname.toString())
        SocketRepository.socket.emit("socket id", nickname)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,0)

        val notification: Notification = NotificationCompat.Builder(this, "exampleServiceChannel")
            .setContentTitle("Foreground Service")
            .setContentText("Running in the background")
            .setSmallIcon(R.drawable.ic_accept)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        // Perform the background task here

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Create the notification channel if Android version is O or higher
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "exampleServiceChannel",
                "Example Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
