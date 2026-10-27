package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ChatMessageEntity::class,
        CapturedNotificationEntity::class,
        RoutineTaskEntity::class,
        LearnedTaskEntity::class,
        UserMemoryEntity::class,
        AssistantRuleEntity::class,
        CallNoteEntity::class,
        AppAnnouncementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MayaDatabase : RoomDatabase() {
    abstract fun mayaDao(): MayaDao

    companion object {
        @Volatile
        private var INSTANCE: MayaDatabase? = null

        fun getInstance(context: Context): MayaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MayaDatabase::class.java,
                    "maya_assistant.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
