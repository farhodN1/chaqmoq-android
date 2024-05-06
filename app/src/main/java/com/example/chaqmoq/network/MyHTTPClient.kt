package com.example.chaqmoq.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MyHTTPClient {
    suspend fun getRequest(urlString: String): String {
        return withContext(Dispatchers.IO) {
            // Perform network operation here
            // For example, using OkHttp
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(urlString)
                .build()

            val response = client.newCall(request).execute()
            return@withContext response.body?.string() ?: ""
        }
    }

    suspend fun postRequest(urlString: String, requestBody: String): String {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBody.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(urlString)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            return@withContext response.body?.string() ?: ""
        }
    }

}
