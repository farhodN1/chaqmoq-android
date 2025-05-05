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
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import com.example.chaqmoq.databinding.ActivityMainBinding
import com.example.chaqmoq.model.User
import com.example.chaqmoq.repos.SocketRepository
import com.example.chaqmoq.ui.chat.ChatViewModel
import com.example.chaqmoq.utils.GlobalVariables.callMaker
import com.example.chaqmoq.utils.GlobalVariables.host
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jakewharton.threetenabp.AndroidThreeTen
import com.permissionx.guolindev.PermissionX
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    var homeViewModel: ChatViewModel? = null
    var userId: String? = null
    val incomingCallAlert = IncomingCallAlert()
    val socket = SocketRepository.socket
    val lastSeenTime = System.currentTimeMillis()
    var lastSeenData: JSONObject? = null
    private lateinit var networkReceiver: NetworkReceiver

    companion object {
        private var appContext: Context? = null
        fun getAppContext(): Context {
            return appContext ?: throw IllegalStateException("Application context not initialized.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = this
        val userInfo = getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
        userId = userInfo.getString("id", "")
        val userName = userInfo.getString("username", "")
        val userEmail = userInfo.getString("email", "")
        val userProfilePicture = userInfo.getString("pictureURL", "")
        Log.d("userId", userId!!)
        host = User(userId!!, userName!!, userEmail, userProfilePicture, null, null, null)
        redirect()
        AndroidThreeTen.init(this)
        socket.connect()
        networkReceiver = NetworkReceiver()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
        homeViewModel = ViewModelProvider(this).get(ChatViewModel::class.java)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
        requestPermissions()

        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        }
        if (userId !== "") {
            initializeSocket()
        }

        val callType = intent.getStringExtra("callType")
        if (callType == "video" || callType == "audio") {
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
        val isLoggedIn: Boolean = getSharedPreferences("UserInfo", Context.MODE_PRIVATE).getBoolean("isLoggedIn", false)
        if (!isLoggedIn) {
            val intent = Intent(this, AuthorizationActivity::class.java)
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

        val homeViewModel = ChatViewModel()
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
            }}
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
        socket.connect()
        if (userId !== null) {
            Log.d("socket", "emitting socket id")
            socket.emit("socket id", userId)
        }
        var navController = findNavController(R.id.nav_host_fragment_activity_main)
        socket.on("audioCall") {msg ->
            SocketRepository.onSocketConnection(this)
            if (msg.isNotEmpty()) {
                val data = JSONObject(msg[0].toString())
                val sender = data.optString("sender")
                val image = data.optString("image")
                callMaker = sender
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
                callMaker = sender
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
        unregisterReceiver(networkReceiver)
    }

    override fun onStop() {
        super.onStop()
        lastSeenData = JSONObject().apply {
            put("userId", host?.id)
            put("time", lastSeenTime.toString())
        }
        if (userId !== null) {
            socket.emit("lastSeen", lastSeenData)
        }
    }
}
