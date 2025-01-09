package com.example.chaqmoq

import android.Manifest
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import com.example.chaqmoq.databinding.ActivityMainBinding
import com.example.chaqmoq.repos.SocketRepository
import com.example.chaqmoq.ui.home.HomeViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jakewharton.threetenabp.AndroidThreeTen
import com.permissionx.guolindev.PermissionX
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    var homeViewModel: HomeViewModel? = null
    var userId: String? = null
    val incomingCallAlert = IncomingCallAlert()
    val socket = SocketRepository.socket
    val lastSeenTime = System.currentTimeMillis()
    var lastSeenData: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = getSharedPreferences("UserInfo", Context.MODE_PRIVATE).getString("nickname", null)
        redirect()
        AndroidThreeTen.init(this)
        socket.connect()
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        }

        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        lastSeenData = JSONObject().apply {
            put("userId", userId)
            put("time", lastSeenTime.toString())
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
        requestPermissions()
        if (userId !== null) {
            initializeSocket()
        }

        val callType = intent.getStringExtra("callType")
        Log.d("callType", callType.toString())
        if (callType == "video" || callType == "audio") {
            Log.d("navigation to", "nav_call")
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            val bundle = Bundle().apply {
                putBoolean("incoming", true)
                putString("callType", callType.toString())
            }
            navController.navigate(R.id.nav_call, bundle)
        }


        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)


        val redirToTarget = intent.getStringExtra("url") ?: null
        Log.d("usernameOf", redirToTarget.toString())
        if (redirToTarget !== null) {
            homeViewModel?.userList?.observe(this) {
                goToTargetUser("https://lh3.googleusercontent.com/a/ACg8ocJOHyhKnjI8sULVur_PATMGYCYPwL8ou_F-BaPa9KT2-G9JIQ=s96-c")
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    private fun redirect() {
        if (userId == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val navView: BottomNavigationView = binding.navView

        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener {_, destination, _ ->
            when (destination.id) {
                R.id.nav_home, R.id.nav_calls, R.id.nav_settings -> {
                    navView.visibility = View.VISIBLE
                }
            else -> {
                navView.visibility = View.GONE
            }
         }
        }
    }

    private fun goToTargetUser(url: String) {
        val targetData = getSharedPreferences("TargetInfo", Context.MODE_PRIVATE)

        val homeViewModel = HomeViewModel()
        val mappedUserList = homeViewModel.userList.map { users ->
            users.map { user ->  {
                if (user.profilePicture == url) {
                    with(targetData.edit()) {
                        putString("id", user.id)
                        putString("username", user.username)
                        putString("email", user.email)
                        putString("pictureURL", user.profilePicture)
                        putString("socket_id", user.socket_id)
                        putString("status", user.status)
                        putString("lastSeen", user.lastSeen)
                        apply()
                    }
                }
            }} // Example transformation
        }

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        navController.navigate(R.id.target_user)

        mappedUserList.observe(this) { transformedUsers ->
            Log.d("Transformed Users", transformedUsers.toString())
        }
    }

    private fun requestPermissions() {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            .request { allGranted, _, _ ->
                if (!allGranted) {
                    Toast.makeText(this, "You should accept all permissions", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun initializeSocket() {
        Log.d("on", "initialize socket")
        socket.connect()
        if (userId !== null) socket.emit("socket id", userId)
        var navController = findNavController(R.id.nav_host_fragment_activity_main)
        socket.on("audioCall") {msg ->
            SocketRepository.onSocketConnection(this)
            if (msg.isNotEmpty()) {
                Log.d("farhod", "${ msg[0] }")
                val data = JSONObject(msg[0].toString())
                val sender = data.optString("sender")
                val image = data.optString("image")
                Log.d("socket", "audioCall ${sender}")
                SocketRepository.callMaker = sender
                runOnMainThread {
                    incomingCallAlert.showWindow(this, sender, image, "audio", navController)
                }
            } else {
                Log.e("socket", "audioCall message array is empty")
            }
        }
        socket.on("videoCall") {msg ->
            SocketRepository.onSocketConnection(this)
            if (msg.isNotEmpty()) {
                val data = JSONObject(msg[0].toString())
                val sender = data.optString("sender")
                val image = data.optString("image")
                Log.d("socket", "call ${sender}")
                SocketRepository.callMaker = sender
                runOnMainThread{
                    incomingCallAlert.showWindow(this, sender, image, "video", navController)
                }
            } else {
                Log.e("socket", "videoCall message array is empty")
            }
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        val mainHandler = Handler(mainLooper)
        mainHandler.post { action() }
    }

    override fun onDestroy() {
        socket.disconnect()
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        Log.d("on", "stop");
        if (userId !== null) {
            socket.emit("lastSeen", lastSeenData)
        }
    }
}
