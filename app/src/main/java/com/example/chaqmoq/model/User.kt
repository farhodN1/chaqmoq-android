package com.example.chaqmoq.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    val username: String,
    val email: String,
    val profilePicture: String? = null,
    val socket_id: String,
    val status: String,
) : Parcelable