package com.example.chaqmoq.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chaqmoq.MainActivity
import com.example.chaqmoq.model.Message

@Database(entities = [Message::class], version = 6)
abstract class MessageListDatabase: RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: MessageListDatabase? = null
        fun isDatabaseInit(): Boolean {
            if (INSTANCE !== null) return true
            else return false
        }
        fun getDatabase(): MessageListDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    MainActivity.getAppContext().applicationContext,
                    MessageListDatabase::class.java,
                    "messages_database"
                    ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}