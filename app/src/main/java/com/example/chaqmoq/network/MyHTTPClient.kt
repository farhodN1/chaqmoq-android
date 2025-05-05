package com.example.chaqmoq.network

import com.example.chaqmoq.repos.SocketRepository
import com.example.chaqmoq.utils.GlobalVariables.ip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MyHTTPClient {
    val serverAddr = ip
    suspend fun getRequest(endpoint: String): String {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(serverAddr + endpoint)
                .build()

            val response = client.newCall(request).execute()
            response.body?.string() ?: ""
        }
    }

    suspend fun postRequest(endpoint: String, requestBody: String): String {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBody.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(serverAddr +"/"+ endpoint)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            return@withContext response.body?.string() ?: ""
        }
    }

}
