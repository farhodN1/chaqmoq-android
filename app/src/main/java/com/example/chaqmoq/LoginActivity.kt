package com.example.chaqmoq

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.example.chaqmoq.databinding.ActivityLoginBinding
import com.example.chaqmoq.network.MyHTTPClient
import com.example.chaqmoq.repos.SocketRepository
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.json.JSONException
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class LoginActivity : Activity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var account: Auth0
    private val myHttpClient = MyHTTPClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        account = Auth0(
            "RQYd3aBfwlWGNtdzrH9UmdaTHQu50Mfo",
            "dev-5jju1y2qxgbshkq7.us.auth0.com"
        )

        binding.loginBtn.setOnClickListener {
            WebAuthProvider.login(account)
                .withScheme("demo")
                .withScope("openid profile email")
                .start(this@LoginActivity, object : Callback<Credentials, AuthenticationException> {
                    override fun onFailure(error: AuthenticationException) {
                        Log.e("error", error.message.toString())
                    }

                    override fun onSuccess(result: Credentials) {
                        val accessToken = result.accessToken
                        showUserProfile(accessToken)
                    }
                })
        }
    }

    private fun showUserProfile(accessToken: String) {
        val client = AuthenticationAPIClient(account)

        client.userInfo(accessToken)
            .start(object : Callback<UserProfile, AuthenticationException> {
                override fun onFailure(error: AuthenticationException) {
                    Log.e("error", error.message.toString())
                }

                override fun onSuccess(result: UserProfile) {
                    Log.i("check", "${result.nickname}")

                    // Prepare the user data to send to the backend
                    val userData = listOf(
                        result.nickname ?: "",
                        result.givenName ?: "",
                        result.pictureURL ?: "",
                        result.email ?: ""
                    )

                    postNetworkRequest(userData)
                }
            })
    }

    private fun postNetworkRequest(userData: List<String>) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val gson = Gson()
                val userInfo = gson.toJson(userData)
                val ip = SocketRepository.ip
                Log.d("req", userInfo)
                val response = myHttpClient.postRequest(ip +"/loggedin", userInfo)
                Log.d("responce", response)
                // Check if the response from the backend is "successful"
                if (response == "successful") {
                    // Save user data to SharedPreferences after successful response
                    val sharedPref = getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("nickname", userData[0])
                        putString("givenName", userData[1])
                        putString("pictureURL", userData[2])
                        putString("email", userData[3])
                        apply()
                    }
                    FirebaseMessaging.getInstance().isAutoInitEnabled = true
                    Log.d("intent", "this is where it should navigate")
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    Log.i("post_response", "Data saved to SharedPreferences")
                } else {
                    Log.e("post_response", "Failed to save data, response: $response")
                }
            } catch (e: JSONException) {
                Log.e("MyTag", "JSON Error: ${e.message}")
            } catch (e: Exception) {
                Log.e("MyTag", "Error: ${e.message}")
            }
        }
    }
}
