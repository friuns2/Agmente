package com.agmente.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ServerEntity::class, SessionEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AgmenteDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AgmenteDatabase? = null

        fun getDatabase(context: Context): AgmenteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AgmenteDatabase::class.java,
                    "agmente_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
