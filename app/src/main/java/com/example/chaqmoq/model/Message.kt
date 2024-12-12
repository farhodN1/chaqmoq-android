package com.example.chaqmoq.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Message(
    val message: String,
    val message_type: String? = null,
    val receiver_id: String,
    val sender_id: String? = null,
    val send_time: String
) : Parcelable