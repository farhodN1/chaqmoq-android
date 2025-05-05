package com.example.chaqmoq.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val email: String?,
    val profilePicture: String? = null,
    val socket_id: String?,
    val status: String?,
    val lastSeen: String?
) : Parcelable