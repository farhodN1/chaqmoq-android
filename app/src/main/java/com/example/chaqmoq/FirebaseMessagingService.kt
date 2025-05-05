package com.example.chaqmoq

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.chaqmoq.repos.SocketRepository
import com.example.chaqmoq.ui.chat.ChatViewModel
import org.json.JSONObject

class FirebaseMessagingService : FirebaseMessagingService() {
    private val channelId = "your_channel_id" // Define your channel ID
    private val channelName = "FCM Notifications" // Define your channel name
    private var incomingCallAlert: IncomingCallAlert? = null
    private lateinit var homeViewModel: ChatViewModel
    var image: Uri? = null

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        incomingCallAlert = IncomingCallAlert()
        homeViewModel = ViewModelProvider.AndroidViewModelFactory(application).create(ChatViewModel::class.java)
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Default Title"
            val body = remoteMessage.data["body"]
            val imageUrl = remoteMessage.data["imageUrl"]
            image = if (imageUrl != null) Uri.parse(imageUrl) else null
            Log.d("FCM", "Message Notification title: $title")
            Log.d("FCM", "Message Notification Body: $body")

            if (title == "incoming" && body !== "") {
                val caller = JSONObject(body)
                Log.d("caller", caller.toString())
                val sender = caller.getString("sender")
                Log.d("sender", sender)
                val image = caller.getString("image")
                val callType = caller.getString("callType")
                incomingCallAlert?.showWindow(this, sender, image, callType, null)
            } else if (image !== null && title !== "incoming") {
                showNotification(title, body!!, imageUrl.toString())
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New Token: $token")
        val sharedPref = getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
        var id = sharedPref.getString("nickname", "")

        val jObject = JSONObject()
        jObject.put("token", token)
        jObject.put("id", id)
        SocketRepository.socket.emit("token", jObject)
        // Send token to your backend or server if necessary
    }

    // Method to show notification
    private fun showNotification(title: String, message: String, imageUrl: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel if needed (for Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel description"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("url", imageUrl)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Build the notification
        val futureTarget: FutureTarget<Bitmap> = Glide.with(applicationContext)
            .asBitmap()
            .load(image)
            .placeholder(R.drawable.roundimage_placeholder)
            .submit()

        val remoteViews = RemoteViews(packageName, R.layout.notification)

        remoteViews.setTextViewText(R.id.notification_title, title)
        remoteViews.setTextViewText(R.id.notification_message, message)
        try {
            remoteViews.setImageViewBitmap(R.id.notification_icon, futureTarget.get())
        } catch (e: Exception) {
            remoteViews.setImageViewResource(R.id.notification_icon, R.drawable.roundimage_placeholder)
        }

        Glide.with(applicationContext).clear(futureTarget)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.roundimage_placeholder)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Show the notification
        notificationManager.notify(0, notificationBuilder.build())
    }

}
