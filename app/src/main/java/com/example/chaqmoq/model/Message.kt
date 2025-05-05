package com.example.chaqmoq.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val conId: String,
    val message: String,
    val message_type: String? = null,
    val amplitudes: String? = null,
    val receiver_id: String,
    val sender_id: String? = null,
    val send_time: String? = null,
    var status: String? = null
) : Parcelable