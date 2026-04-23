// com/example/prob1/data/database/AppDatabase.kt
package com.example.prob1.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.prob1.data.database.dao.*
import com.example.prob1.data.database.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TestEntity::class,
        PartEntity::class,
        QuestionEntity::class,
        AnswerEntity::class,
        LectionEntity::class,
        CourseEntity::class,
        CourseGroupEntity::class,
        UserDataEntity::class,
        SyncInfoEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun testDao(): TestDao
    abstract fun partDao(): PartDao
    abstract fun questionDao(): QuestionDao
    abstract fun answerDao(): AnswerDao  // ← ДОБАВЛЕНО
    abstract fun lectionDao(): LectionDao
    abstract fun courseDao(): CourseDao
    abstract fun userDataDao(): UserDataDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prob1_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    // Инициализация при создании БД
                }
            }
        }
    }
}