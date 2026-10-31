package com.example.evoke.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.example.evoke.security.DatabaseKeyManager

@Database(
    entities = [
        User::class,
        Person::class,
        FaceEmbedding::class,
        VoiceEmbedding::class,
        UserVoiceEmbedding::class,
        RecognitionEvent::class,
        Conversation::class,
        PersonHistory::class,
        PendingConversation::class,
        Location::class,
        EmergencyContact::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun personDao(): PersonDao
    abstract fun faceEmbeddingDao(): FaceEmbeddingDao
    abstract fun voiceEmbeddingDao(): VoiceEmbeddingDao
    abstract fun userVoiceEmbeddingDao(): UserVoiceEmbeddingDao
    abstract fun recognitionEventDao(): RecognitionEventDao
    abstract fun conversationDao(): ConversationDao
    abstract fun personHistoryDao(): PersonHistoryDao
    abstract fun pendingConversationDao(): PendingConversationDao
    abstract fun locationDao(): LocationDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Opens the normal local database using the locally stored
         * SQLCipher database key.
         */
        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val databaseKey =
                    DatabaseKeyManager(context).getDatabaseKey()

                createDatabase(
                    context = context,
                    databaseKey = databaseKey
                )
            }
        }

        /**
         * Opens the database using an explicitly supplied key.
         *
         * Used when restoring a database from backup.
         */
        fun getDatabaseWithKey(
            context: Context,
            databaseKey: ByteArray
        ): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                createDatabase(
                    context = context,
                    databaseKey = databaseKey
                )
            }
        }

        private fun createDatabase(
            context: Context,
            databaseKey: ByteArray
        ): AppDatabase {

            require(databaseKey.size == 32) {
                "Database key must be 32 bytes."
            }

            val factory =
                SupportOpenHelperFactory(databaseKey)

            val instance =
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dementia_database"
                )
                    .openHelperFactory(factory)
                    .build()

            INSTANCE = instance

            return instance
        }

        fun checkpointDatabase() {
            INSTANCE?.openHelper?.writableDatabase?.query(
                "PRAGMA wal_checkpoint(FULL)"
            )?.use {
                // Checkpoint executed.
            }
        }


        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}