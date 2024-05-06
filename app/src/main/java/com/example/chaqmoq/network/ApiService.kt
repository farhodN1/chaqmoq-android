package com.example.chaqmoq.network

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface MyApiService {
    @GET
    fun getData(@Url url: String): Call<String>

    @POST
    fun postData(@Url url: String, @Body requestBody: RequestBody): Call<String>
}
