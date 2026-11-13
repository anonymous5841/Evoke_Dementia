package com.example.evoke.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.evoke.security.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.util.concurrent.Callable

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
    version = 3,
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

        private const val DATABASE_NAME = "evoke_database"
        private const val QUIESCE_ATTEMPTS = 3

        fun isOpen(): Boolean = INSTANCE != null

        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var nativeLibraryLoaded = false

        // Version 1 -> 2: audio path and processing status for pending conversations.
        //
        // NOTE: this only works if the v1 pending_conversations table has no
        // column that the current entity no longer has (for example an old raw
        // conversation text column). If it did, recreate the table instead.
        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    ALTER TABLE pending_conversations
                    ADD COLUMN audioPath TEXT
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    ALTER TABLE pending_conversations
                    ADD COLUMN status TEXT NOT NULL
                    DEFAULT 'WAITING_TRANSCRIPTION'
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_conversations_recognitionId")
                db.execSQL("CREATE UNIQUE INDEX index_conversations_recognitionId ON conversations(recognitionId)")

                db.execSQL("DROP INDEX IF EXISTS index_person_history_recognitionId")
                db.execSQL("CREATE UNIQUE INDEX index_person_history_recognitionId ON person_history(recognitionId)")

                db.execSQL("DROP INDEX IF EXISTS index_pending_conversations_conversationId")
                db.execSQL("CREATE UNIQUE INDEX index_pending_conversations_conversationId ON pending_conversations(conversationId)")

                db.execSQL("CREATE UNIQUE INDEX index_emergency_contacts_priority ON emergency_contacts(priority)")
            }
        }
        /**
         * Opens the normal local database using the locally stored
         * SQLCipher database key.
         */
        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val databaseKey =
                        DatabaseKeyManager(context).getDatabaseKey()

                    createDatabase(
                        context = context,
                        databaseKey = databaseKey
                    )
                }
            }
        }

        /**
         * Opens the database using an explicitly supplied key.
         *
         * Used when restoring a database from backup. The current instance
         * must be closed first, otherwise the supplied key would be silently
         * ignored.
         */
        fun getDatabaseWithKey(
            context: Context,
            databaseKey: ByteArray
        ): AppDatabase {

            return synchronized(this) {

                check(INSTANCE == null) {
                    "Close the existing database before opening it with another key."
                }

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

            // SQLCipher's native library must be loaded before first use.
            // Doing it here means it no longer depends on Application.onCreate.
            if (!nativeLibraryLoaded) {
                System.loadLibrary("sqlcipher")
                nativeLibraryLoaded = true
            }

            val factory =
                SupportOpenHelperFactory(databaseKey)

            val instance =
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

            INSTANCE = instance

            return instance
        }

        fun checkpointDatabase() {
            INSTANCE?.openHelper?.writableDatabase?.query(
                "PRAGMA wal_checkpoint(TRUNCATE)"
            )?.use {
                // Checkpoint executed.
            }
        }

        /**
         * Runs [block] while the database FILE is guaranteed to be complete
         * and unchanging, WITHOUT closing the database.
         *
         * Used for creating backups. Closing the database (the old approach)
         * left every repository, DAO and ViewModel pointing at a closed
         * instance.
         *
         * How it works:
         * 1. Checkpoint the WAL into the main database file.
         * 2. Take the write lock (a Room transaction), so no one can write.
         * 3. Verify the WAL is empty. If something wrote in between, release
         *    the lock and try again.
         *
         * Must not be called on the main thread.
         */
        fun <T> withQuiescedDatabase(
            context: Context,
            block: () -> T
        ): T {

            val database = getDatabase(context)
            val sqliteDatabase = database.openHelper.writableDatabase

            val walFile = File(
                requireNotNull(sqliteDatabase.path) {
                    "Database path is unavailable."
                } + "-wal"
            )

            repeat(QUIESCE_ATTEMPTS) {

                sqliteDatabase.query(
                    "PRAGMA wal_checkpoint(TRUNCATE)"
                ).use { cursor ->
                    cursor.moveToFirst()
                }

                val result = database.runInTransaction(
                    Callable<Quiesced<T>?> {

                        if (walFile.exists() && walFile.length() > 0L) {
                            null
                        } else {
                            Quiesced(block())
                        }
                    }
                )

                if (result != null) {
                    return result.value
                }
            }

            throw IllegalStateException(
                "The database kept changing while preparing the backup. Please try again."
            )
        }

        private class Quiesced<T>(val value: T)

        /**
         * Closes the database. Only use this when the app is about to
         * replace the database file (restore) or wipe all data, and
         * recreate its repositories / ViewModels afterwards (or restart).
         */
        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
