package com.example.chaqmoq.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.chaqmoq.MainActivity
import com.example.chaqmoq.model.User

@Database(entities = [User::class], version = 2)
abstract class UserlistDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: UserlistDatabase? = null

        fun getDatabase(): UserlistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    MainActivity.getAppContext().applicationContext,
                    UserlistDatabase::class.java,
                    "users_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}