package com.example.chaqmoq.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chaqmoq.model.User


@Dao
interface UserDao {
    @Query("SELECT * FROM users ")
    fun getAllUsers(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveUsers(users: List<User>)

    @Query("DELETE FROM users")
    suspend fun clearAllUsers()
}