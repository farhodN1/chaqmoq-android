package com.example.chaqmoq

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.example.chaqmoq.databinding.ActivityLoginBinding
import com.example.chaqmoq.model.User
import com.example.chaqmoq.network.MyHTTPClient
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.json.JSONException
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class AuthorizationActivity : Activity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var account: Auth0
    private val myHttpClient = MyHTTPClient()
    private lateinit var endpoint: String

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
            endpoint = "login"
            openChromeAuthenticator()
        }
        binding.signinBtn.setOnClickListener {
            endpoint = "signin"
            openChromeAuthenticator()
        }
    }

    private fun openChromeAuthenticator() {
        WebAuthProvider.login(account)
            .withScheme("demo")
            .withScope("openid profile email")
            .start(this@AuthorizationActivity, object : Callback<Credentials, AuthenticationException> {
                override fun onFailure(error: AuthenticationException) {
                    displayToast("Something went wrong, please try again")
                }

                override fun onSuccess(result: Credentials) {
                    val accessToken = result.accessToken
                    showUserProfile(accessToken)
                }
            })
    }

    private fun showUserProfile(accessToken: String) {
        val client = AuthenticationAPIClient(account)

        client.userInfo(accessToken)
            .start(object : Callback<UserProfile, AuthenticationException> {
                override fun onFailure(error: AuthenticationException) {

                }

                override fun onSuccess(result: UserProfile) {
                    // Prepare the user data to send to the backend
                    val user = User(
                        result.nickname ?: "",
                        result.givenName ?: "",
                        result.email ?: "",
                        result.pictureURL ?: "",
                        null,
                        null,
                        null
                    )
                    postUserInfo(user)
                }
            })
    }

    private fun postUserInfo(user: User) {
        val storedUserInfo = getSharedPreferences("UserInfo", Context.MODE_PRIVATE)
        if (::endpoint.isInitialized) {
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val gson = Gson()
                    val userInfo = gson.toJson(user)
                    Log.d("endpoint", endpoint)
                    val response = myHttpClient.postRequest(endpoint, userInfo)
                    if (response == "successful") {
                        with(storedUserInfo.edit()) {
                            putBoolean("isLoggedIn", true)
                            putString("id", user.id)
                            putString("username", user.username)
                            putString("pictureURL", user.profilePicture)
                            putString("email", user.email)
                            apply()
                        }
                        FirebaseMessaging.getInstance().isAutoInitEnabled = true
                        val intent = Intent(this@AuthorizationActivity, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        displayToast("Failed to save data, you have to log in again")
                    }
                } catch (e: JSONException) {
                    displayToast("Login failed, please try again!")
                } catch (e: Exception) {
                    displayToast("Login failed, please try again!")
                }
            }
        }
    }
    fun displayToast(text: String) {
        if (applicationContext !== null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
            }
        }
    }
}
