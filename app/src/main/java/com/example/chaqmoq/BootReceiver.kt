package com.example.chaqmoq

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Start the foreground service
            val serviceIntent = Intent(context, MainActivity::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
