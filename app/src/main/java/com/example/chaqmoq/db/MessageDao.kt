package com.example.chaqmoq.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chaqmoq.model.Message

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages")
    fun getAllMessages(): List<Message>

    @Query("SELECT * FROM messages WHERE status = 'pending'")
    suspend fun getUnsentMessages(): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveMessages(messages: List<Message>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMessage(messages: Message)

    @Query("DELETE FROM messages")
    fun deleteAllConversations()

    @Query("DELETE FROM messages WHERE conId = :conId")
    fun deleteConversationByConId(conId: String)

    @Query("SELECT * FROM messages WHERE conId = :conId")
    fun getConversationByConId(conId: String): List<Message>

}