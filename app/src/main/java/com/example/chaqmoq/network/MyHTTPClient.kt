package com.example.chaqmoq.network

import com.example.chaqmoq.repos.SocketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MyHTTPClient {
    suspend fun getRequest(urlString: String): String {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(urlString)
                .build()

            val response = client.newCall(request).execute()
            response.body?.string() ?: ""
        }
    }

    suspend fun postRequest(endpoint: String, requestBody: String): String {
        val serverAddr = SocketRepository.ip
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
