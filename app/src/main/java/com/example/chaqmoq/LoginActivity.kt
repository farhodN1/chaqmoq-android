package com.example.chaqmoq

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.example.chaqmoq.databinding.ActivityLoginBinding
import com.example.chaqmoq.network.MyHTTPClient
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.json.JSONException

class LoginActivity : AppCompatActivity() {

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
                    val sharedPref = getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("nickname", result.nickname)
                        putString("givenName", result.givenName)
                        putString("pictureURL", result.pictureURL)
                        // Add other user info as needed
                        apply()
                    }
                    // Ensure all elements are of type String
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

    private fun postNetworkRequest(data: List<String>) {
        lifecycleScope.launch {
            try {
                val gson = Gson()
                val json = gson.toJson(data)
                val response = myHttpClient.postRequest("http://192.168.1.7:5000/loggedin", json)
                Log.i("post_response", "Response: $response")
            } catch (e: JSONException) {
                Log.e("MyTag", "JSON Error: ${e.message}")
            } catch (e: Exception) {
                Log.e("MyTag", "Error: ${e.message}")
            }
        }
    }


}
