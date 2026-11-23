package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [OutageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OutageDatabase : RoomDatabase() {
    abstract fun outageDao(): OutageDao

    companion object {
        @Volatile
        private var INSTANCE: OutageDatabase? = null

        fun getInstance(context: Context): OutageDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OutageDatabase::class.java,
                    "se_fue_la_luz_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
