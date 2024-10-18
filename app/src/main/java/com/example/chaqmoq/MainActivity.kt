package com.example.chaqmoq

import android.Manifest
import android.content.BroadcastReceiver
import android.os.Bundle
import android.view.Menu
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.example.chaqmoq.databinding.ActivityMainBinding
import com.example.chaqmoq.repos.SocketRepository
import com.example.chaqmoq.repos.WebRTCRepository
import com.example.chaqmoq.ui.targetUser.TargetUserFragment
import com.example.chaqmoq.ui.targetUser.TargetUserFragmentDirections
import com.permissionx.guolindev.PermissionX
import io.socket.client.Socket
import org.webrtc.SurfaceViewRenderer


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.chaqmoq.CALL_RECEIVED") {
                // Trigger navigation when the broadcast is received
                val callType = intent.getStringExtra("callType")
                runOnUiThread {
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    val bundle = Bundle().apply {
                        putBoolean("incoming", true)
                        putString("callType", callType.toString())
                    }
                    navController.navigate(R.id.nav_call, bundle)
                }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
        var nickname = sharedPref.getString("nickname", null)
        SocketRepository.socket.on(Socket.EVENT_CONNECT) {SocketRepository.socket.emit("socket id", nickname)}
        val givenName = sharedPref.getString("givenName", null)
        val pictureURL = sharedPref.getString("pictureURL", null)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)


        val intent = Intent(this, CallService::class.java)
        intent.putExtra("username", nickname)
        startService(intent)
        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
            .request{ allGranted, _ ,_ ->
                if (allGranted){
                    setSupportActionBar(binding.appBarMain.toolbar)
                } else {
                    Toast.makeText(this,"you should accept all permissions", Toast.LENGTH_LONG).show()
                }
            }

        SocketRepository.onSocketConnection()
        SocketRepository.socket.emit("socket id", nickname)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val filter = IntentFilter("com.example.chaqmoq.CALL_RECEIVED")
        registerReceiver(callReceiver, filter)
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        if (nickname == null) {
            navController.navigate(R.id.login_activity)
        }

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_call
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(callReceiver)
    }
}