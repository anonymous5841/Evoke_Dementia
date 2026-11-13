package com.example.evoke.database.repository

import androidx.room.withTransaction
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.Conversation
import com.example.evoke.database.PendingConversation
import com.example.evoke.database.PendingConversationStatus
import com.example.evoke.database.PersonHistory
import com.example.evoke.database.RecognitionEvent
import com.example.evoke.storage.AudioStorage

/**
 * Result of saving a recognition.
 *
 * [audioQueued] is false when an audio file was supplied but NOT queued,
 * because voice is disabled for the person or the pending queue is full.
 * The repository deletes the file in that case.
 */
internal data class RecognitionSaveResult(
    val recognitionId: Long?,
    val audioQueued: Boolean
)

class RecognitionRepository(
    private val db: AppDatabase,
    private val audioStorage: AudioStorage
) {

    private val recognitionDao = db.recognitionEventDao()
    private val historyDao = db.personHistoryDao()
    private val conversationDao = db.conversationDao()
    private val pendingDao = db.pendingConversationDao()
    private val personDao = db.personDao()

    companion object {
        const val MAX_PENDING_CONVERSATIONS = 5

    }

    /**
     * Creates a recognition and its related records atomically.
     *
     * The repository takes ownership of [audioPath]'s file: it is deleted
     * if saving fails, or if the recording cannot be queued (voice disabled
     * or queue full). Check [isPendingQueueFull] beforehand if the UI should
     * warn the user.
     */
    suspend fun createRecognition(
        personId: Long,
        dateTime: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        audioPath: String? = null
    ): Long {

        val hasLocation = latitude != null && longitude != null

        if (!hasLocation && audioPath.isNullOrBlank()) {
            throw IllegalArgumentException(
                "Recognition must contain either location or a conversation recording."
            )
        }

        val result = try {

            db.withTransaction {
                createRecognitionInternal(
                    personId = personId,
                    dateTime = dateTime,
                    latitude = latitude,
                    longitude = longitude,
                    audioPath = audioPath
                )
            }

        } catch (exception: Throwable) {

            // The transaction rolled back, so nothing references the file.
            audioStorage.deleteAudio(audioPath)

            throw exception
        }

        if (!result.audioQueued) {
            audioStorage.deleteAudio(audioPath)
        }

        return result.recognitionId
            ?: throw IllegalStateException(
                "Recognition was not saved: no location and the conversation could not be queued."
            )
    }

    /**
     * Internal version used when another repository already owns
     * the database transaction, such as PersonRepository.
     *
     * Does NOT delete audio files: the transaction owner does that after
     * it knows whether the transaction committed.
     *
     * A recognition event is always created. without a location or a queued conversation
     * it returns recognitionId = null, and the caller decides what to do. The allowEmpty
     * parameter can stay (default false) or you can delete it, since nothing will use it.
     */
    internal suspend fun createRecognitionInternal(
        personId: Long,
        dateTime: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        audioPath: String? = null,
        allowEmpty: Boolean = false
    ): RecognitionSaveResult {

        validateCoordinates(latitude, longitude)

        var queueAudio = !audioPath.isNullOrBlank()

        if (queueAudio) {

            val voiceEnabled = personDao.isVoiceEnabled(personId)
                ?: throw IllegalArgumentException(
                    "Person does not exist."
                )

            // Enforced inside the transaction, so a sixth pending
            // conversation can never be inserted. When voice is off or the
            // queue is full the recording is skipped, not treated as an error.
            queueAudio =
                voiceEnabled &&
                        pendingDao.getPendingCount() < MAX_PENDING_CONVERSATIONS
        }

        // A recognition needs a location or a queued conversation.
        if (!allowEmpty && !(latitude != null && longitude != null) && !queueAudio) {
            return RecognitionSaveResult(recognitionId = null, audioQueued = false)
        }

        val recognitionId = recognitionDao.insert(
            RecognitionEvent(
                personId = personId,
                dateTime = dateTime
            )
        )

        // Save location history when both coordinates are available.
        if (latitude != null && longitude != null) {
            historyDao.insert(
                PersonHistory(
                    recognitionId = recognitionId,
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }

        // Create a pending conversation when an audio recording is queued.
        if (queueAudio && audioPath != null) {

            val conversationId = conversationDao.insert(
                Conversation(
                    recognitionId = recognitionId,
                    summary = ""
                )
            )

            pendingDao.insert(
                PendingConversation(
                    conversationId = conversationId,
                    audioPath = audioPath
                    // Status defaults to WAITING_TRANSCRIPTION.
                )
            )
        }

        return RecognitionSaveResult(
            recognitionId = recognitionId,
            audioQueued = queueAudio
        )
    }

    /**
     * Saves the transcription and advances the pending conversation to the
     * summarization stage in ONE transaction. The same transaction clears
     * audioPath. The audio file is deleted after the transaction commits.
     *
     * The raw transcript is stored temporarily in Conversation.summary and is
     * replaced by the real summary in completeConversation(). The audio path is
     * cleared in the transaction and the file is deleted after commit; if that
     * delete fails, cleanupOrphanedAudio() removes it at the next start.
     */
    suspend fun saveTranscription(
        pendingId: Long,
        conversationId: Long,
        transcription: String
    ) {
        require(transcription.isNotBlank()) { "Transcription cannot be empty" }

        val audioPathToDelete = db.withTransaction {

            val pending = pendingDao.getOldestPending()
                ?: throw IllegalStateException("No pending conversation exists.")

            check(pending.pendingId == pendingId &&
                    pending.conversationId == conversationId) {
                "Pending conversation does not match."
            }

            check(pending.status == PendingConversationStatus.WAITING_TRANSCRIPTION) {
                "Pending conversation was not in WAITING_TRANSCRIPTION state."
            }

            // The raw transcript lives in summary until completeConversation().
            check(conversationDao.updateSummary(conversationId, transcription) == 1) {
                "Conversation does not exist."
            }

            check(pendingDao.markWaitingForSummary(pendingId) == 1) {
                "Unable to advance pending conversation to summarization."
            }

            check(pendingDao.clearAudioPath(pendingId) == 1) {
                "Unable to clear audio path."
            }

            pending.audioPath
        }

        audioStorage.deleteAudio(audioPathToDelete)
    }
    /**
     * Saves the final summary, discards the raw transcript and removes
     * the pending entry atomically.
     */
    suspend fun completeConversation(
        conversationId: Long,
        summary: String
    ) {

        require(summary.isNotBlank()) {
            "Summary cannot be empty."
        }

        val wordCount = summary.trim().split(Regex("\\s+")).size

        require(wordCount <= 250) {
            "Summary exceeds the 250-word limit."
        }

        db.withTransaction {

            val pending = pendingDao.getOldestPending()
                ?: throw IllegalStateException("No pending conversation exists.")

            check(pending.conversationId == conversationId) {
                "This conversation is not the next one in the queue."
            }

            check(pending.status == PendingConversationStatus.WAITING_SUMMARY) {
                "Conversation has not been transcribed yet."
            }

            check(conversationDao.updateSummary(conversationId, summary) == 1) {
                "Conversation does not exist."
            }
            check(pendingDao.deleteById(pending.pendingId) == 1) {
                "Unable to remove the completed pending entry."
            }

        }
    }


    /**
     * Drops a pending item immediately, for example when its audio file
     * is missing. The conversation row stays with an empty summary.
     */
    suspend fun discardPending(pendingId: Long) {

        val audioPathToDelete = db.withTransaction {
            val pending = pendingDao.getOldestPending()
                ?.takeIf { it.pendingId == pendingId }
                ?: return@withTransaction null

            pendingDao.deleteById(pendingId)
            pending.audioPath
        }

        audioStorage.deleteAudio(audioPathToDelete)
    }

    // Returns the total number of entries in the pending table,
    // regardless of whether they are waiting for transcription or summary.
    suspend fun getPendingConversationCount(): Int {
        return pendingDao.getPendingCount()
    }

    // True when no more recordings can be queued right now.
    suspend fun isPendingQueueFull(): Boolean {
        return pendingDao.getPendingCount() >= MAX_PENDING_CONVERSATIONS
    }

    // Deletes the recognition and any audio file still waiting for it.
    suspend fun deleteRecognition(
        recognitionId: Long
    ) {

        val audioPaths = db.withTransaction {

            val paths =
                pendingDao.getAudioPathsForRecognition(recognitionId)

            recognitionDao.deleteById(recognitionId)

            paths
        }

        audioPaths.forEach { path ->
            audioStorage.deleteAudio(path)
        }
    }

    // Returns only the oldest pending conversation.
    suspend fun getOldestPending(): PendingConversation? {
        return pendingDao.getOldestPending()
    }

    // Retrieves the conversation associated with a pending entry.
    // For a WAITING_SUMMARY entry, conversation.summary holds the raw transcript
    // that has to be summarized.
    suspend fun getConversation(
        conversationId: Long
    ): Conversation? {
        return conversationDao.getById(conversationId)
    }

    // True while a conversation is still waiting for transcription/summary.
    suspend fun isConversationPending(
        conversationId: Long
    ): Boolean {
        return pendingDao.isPending(conversationId)
    }

    // Clears the stored audio path after the audio file has been deleted.
    suspend fun clearPendingAudioPath(
        pendingId: Long
    ): Boolean {
        return pendingDao.clearAudioPath(pendingId) == 1
    }

    /**
     * Call once at app start, BEFORE any recording is saved.
     * Removes half-copied files and audio files that no database row
     * references any more.
     */
    suspend fun cleanupOrphanedAudio() {
        audioStorage.deleteTemporaryFiles()
        audioStorage.deleteUnreferencedAudio(
            pendingDao.getAllAudioPaths()
        )
    }
}
