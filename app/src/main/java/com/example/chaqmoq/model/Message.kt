package com.example.chaqmoq.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Message(
    val message: String,
    val receiver_id: String,
    val sender_id: String? = null
) : Parcelable