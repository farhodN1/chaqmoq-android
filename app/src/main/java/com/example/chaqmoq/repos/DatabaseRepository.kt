package com.example.chaqmoq.repos

import com.example.chaqmoq.db.MessageDao
import com.example.chaqmoq.db.MessageListDatabase
import com.example.chaqmoq.model.User
import com.example.chaqmoq.db.UserDao
import com.example.chaqmoq.db.UserlistDatabase
import com.example.chaqmoq.model.Message

object DatabaseRepository {
    private val usersDao: UserDao by lazy {
        UserlistDatabase.getDatabase().userDao()
    }

    private val messagesDao: MessageDao by lazy {
        MessageListDatabase.getDatabase().messageDao()
    }

    fun saveUsers(users: List<User>) {
        usersDao.saveUsers(users)
    }

    suspend fun deleteUsers() {
        usersDao.clearAllUsers()
    }

    fun getAllUsers(): List<User> {
        return usersDao.getAllUsers()
    }

    fun saveMessages(messages: List<Message>) {
        messagesDao.saveMessages(messages)
    }

    suspend fun saveMessage(message: Message) {
        messagesDao.saveMessage(message)
    }

    fun getAllMessages(): List<Message> {
        return messagesDao.getAllMessages()
    }

    suspend fun getUnsentMessage(): List<Message> {
        return messagesDao.getUnsentMessages()
    }

    fun getConversationByConId(conId: String): List<Message> {
        return messagesDao.getConversationByConId(conId)
    }

    fun deleteConversationByConId(conId: String) {
        messagesDao.deleteConversationByConId(conId)
    }

    fun deleteAllConversations() {
        messagesDao.deleteAllConversations()
    }
}