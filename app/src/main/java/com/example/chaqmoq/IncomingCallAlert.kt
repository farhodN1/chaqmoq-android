package com.example.chaqmoq

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chaqmoq.repos.SocketRepository
import kotlin.math.abs

class IncomingCallAlert {


    companion object {
        private const val WINDOW_WIDTH_RATIO = 1f
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
        var isViewAdded: Boolean = false
    }

    private lateinit var windowManager: WindowManager
    private var player: MediaPlayer? = null
    private var windowLayout: ViewGroup? = null
    private var gestureDetector: GestureDetector? = null
    private var isWindowLayoutActive = true

    private var params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        windowType,
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP
    }
    private var x = 0f
    private var y = 0f

    private val windowType: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private val WindowManager.windowWidth: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = currentWindowMetrics
            val insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            (WINDOW_WIDTH_RATIO * (windowMetrics.bounds.width() - insets.left - insets.right)).toInt()
        } else {
            DisplayMetrics().apply {
                defaultDisplay?.getMetrics(this)
            }.run {
                (WINDOW_WIDTH_RATIO * widthPixels).toInt()
            }
        }


    fun showWindow(context: Context, username: String, image: String?, callType: String, navigation: NavController?) {
        Log.d("navExistence", navigation.toString())
        SocketRepository.socket.on("endCall") {
            closeWindow()
            stopRingtone()
            isViewAdded = false
        }
        if (Settings.canDrawOverlays(context)) {
            Log.d("isViewAdded", isViewAdded.toString())
            if (isViewAdded == false) {
                isViewAdded = true
                windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowLayout = View.inflate(context, R.layout.window_call_info, null) as ViewGroup
                windowLayout?.findViewById<TextView>(R.id.userName)?.text = username
                windowLayout?.findViewById<TextView>(R.id.callType)?.text = callType
                val userImage = windowLayout!!.findViewById<ImageView>(R.id.userImage)
                val handler = Handler(Looper.getMainLooper())


                handler.post {
                    if (image !== null) {
                        Glide.with(context)
                            .load(image)
                            .placeholder(R.drawable.roundimage_placeholder)
                            .error(R.drawable.roundimage_placeholder)
                            .apply(RequestOptions.circleCropTransform())
                            .into(userImage)
                    }
                    gestureDetector = GestureDetector(context, GestureListener())

                    windowLayout?.let {
                        params.width = windowManager.windowWidth
                        val acceptButton = it.findViewById<Button>(R.id.accept)
                        val cancelButton = it.findViewById<Button>(R.id.decline)

                        cancelButton.setOnClickListener {
                            closeWindow()
                            stopRingtone()
                            SocketRepository.socket.emit("endCall")
                        }

                        acceptButton.setOnClickListener {
                            closeWindow()
                            stopRingtone()
                            if (navigation !== null) {
                                navigation.navigate(R.id.nav_call, Bundle().apply {
                                    putString("callType", callType)
                                    putBoolean("incoming", true)
                                })
                            } else {
                                openApp(context, callType)
                            }
                        }
                        windowManager.addView(it, params)
                        setOnTouchListener()
                        playRingtone(context)
                    }
                }
            } else {
                // here you might send that user is currently busy
            }
        } else {
            // Show Toast on the main thread
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Please grant overlay permission", Toast.LENGTH_SHORT).show()
            }

            // Request permission with FLAG_ACTIVITY_NEW_TASK
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }


    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            e1 ?: return false // Handle null case
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            if (abs(diffX) > abs(diffY) && abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    onSwipeRight()
                } else {
                    onSwipeLeft()
                }
                return true
            }
            return false
        }


    }

    private fun onSwipeRight() {
        closeWindow()
        stopRingtone()
        isWindowLayoutActive = false
    }

    private fun onSwipeLeft() {
        closeWindow()
        stopRingtone()
        isWindowLayoutActive = false
    }

    private fun playRingtone(context: Context) {
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = MediaPlayer.create(context, ringtoneUri).apply {
            isLooping = true
            start()
        }
    }

    private fun openApp(context: Context, callType: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("callType", callType)
        }
        Log.d("on", "initializing main activity to reply the call")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun stopRingtone() {
        // Stop and release MediaPlayer when done
        player?.apply {
            if (isPlaying) stop()
            release()
        }
        player = null
    }

    fun closeWindow() {
        windowLayout?.let {
            windowManager.removeView(it)
            windowLayout = null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListener() {
        windowLayout?.setOnTouchListener { view: View, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x = event.rawX
//                    y = event.rawY
                }
                MotionEvent.ACTION_MOVE -> updateWindowLayoutParams(event)

                MotionEvent.ACTION_UP -> checkDismissed()
            }
            gestureDetector?.onTouchEvent(event)

            true
        }
    }

    private fun updateWindowLayoutParams(event: MotionEvent) {
        params.x -= (x - event.rawX).toInt()
//        params.y -= (y - event.rawY).toInt()
        windowLayout?.let {
            windowManager.updateViewLayout(it, params)
        }

//        x = event.rawX
        y = event.rawY
    }

    private fun checkDismissed() {
        if (isWindowLayoutActive) {
            params.x = 0
            windowManager.updateViewLayout(windowLayout, params)
        }
    }
}
