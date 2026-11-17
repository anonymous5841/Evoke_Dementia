package com.example.evoke.database.repository

import androidx.room.withTransaction
import com.example.evoke.database.FaceEmbedding
import com.example.evoke.database.Person
import com.example.evoke.database.PersonDetails
import com.example.evoke.database.RecognitionDetails
import com.example.evoke.database.VoiceEmbedding
import com.example.evoke.database.AppDatabase
import com.example.evoke.storage.AudioStorage
import com.example.evoke.storage.PersonImageStorage

class PersonRepository(
    private val db: AppDatabase,
    private val recognitionRepository: RecognitionRepository,
    private val audioStorage: AudioStorage,
    private val imageStorage: PersonImageStorage
) {

    private val personDao = db.personDao()
    private val faceEmbeddingDao = db.faceEmbeddingDao()
    private val voiceEmbeddingDao = db.voiceEmbeddingDao()
    private val recognitionDao = db.recognitionEventDao()
    private val historyDao = db.personHistoryDao()
    private val conversationDao = db.conversationDao()
    private val pendingDao = db.pendingConversationDao()

    companion object {
        const val MAX_PERSONS = 100
    }
    /**
     * Registers a person, their first embeddings and the first recognition
     * in ONE transaction.
     *
     * [profileImageProvider] is called inside the transaction, right after
     * the person row exists, with the new personId. It must save the profile
     * image and return its relative path. That way the image file name can
     * use the real personId, Person.profileImagePath is never left empty,
     * and if anything later in the transaction fails, the image is deleted.
     *
     * The repository takes ownership of [audioPath]'s file: it is deleted if
     * registration fails or the recording cannot be queued. Registration
     * itself never fails because the pending queue is full or GPS is missing.
     *
     *
     *  * A person must have a first recognition with a location or a queued
     *  * conversation. If neither is available (no GPS, voice disabled, or the
     *  * pending queue is full) registration fails and everything is rolled back.
     */
    suspend fun createPerson(
        person: Person,
        firstFaceEmbedding: FaceEmbedding,
        dateTime: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        audioPath: String? = null,
        voiceEmbedding: VoiceEmbedding? = null,
        profileImageProvider: ((personId: Long) -> String)? = null,
        // Up to 2 more voice samples recorded at registration (3 in total), saved in
        // the same transaction. Requires [voiceEmbedding].
        additionalVoiceEmbeddings: List<VoiceEmbedding> = emptyList()
    ): Long {

        require(additionalVoiceEmbeddings.isEmpty() || voiceEmbedding != null) {
            "additionalVoiceEmbeddings needs a first voiceEmbedding."
        }
        require(additionalVoiceEmbeddings.size <= 2) {
            "A maximum of 3 voice embeddings are allowed."
        }

        var savedImagePath: String? = null

        val saved = try {

            db.withTransaction {

                // Prevent registration when the maximum number of persons is reached.
                check(personDao.getPersonCount() < MAX_PERSONS) {
                    "The maximum limit of $MAX_PERSONS persons has been reached."
                }

                val personId = personDao.insert(
                    person.copy(
                        voiceEnabled = voiceEmbedding != null
                    )
                )

                if (profileImageProvider != null) {

                    val imagePath = profileImageProvider(personId)

                    savedImagePath = imagePath

                    personDao.updatePersonDetails(
                        personId = personId,
                        name = person.name,
                        relation = person.relation,
                        profileImagePath = imagePath
                    )
                }

                faceEmbeddingDao.insert(
                    firstFaceEmbedding.copy(
                        personId = personId
                    )
                )

                if (voiceEmbedding != null) {
                    voiceEmbeddingDao.insert(
                        voiceEmbedding.copy(
                            personId = personId
                        )
                    )
                }

                additionalVoiceEmbeddings.forEach { extra ->
                    voiceEmbeddingDao.insert(
                        extra.copy(
                            personId = personId
                        )
                    )
                }

                val recognition =
                    recognitionRepository.createRecognitionInternal(
                        personId = personId,
                        dateTime = dateTime,
                        latitude = latitude,
                        longitude = longitude,
                        audioPath = audioPath
                    )

                check(recognition.recognitionId != null) {
                    "A person needs a location or a recorded conversation."
                }

                personId to recognition.audioQueued
            }

        } catch (exception: Throwable) {

            // The transaction rolled back: remove the files it created.
            val imagePath = savedImagePath

            if (imagePath != null) {
                imageStorage.deleteProfileImage(imagePath)
            }

            audioStorage.deleteAudio(audioPath)

            throw exception
        }

        val (personId, audioQueued) = saved

        if (!audioQueued) {
            audioStorage.deleteAudio(audioPath)
        }

        return personId
    }

    suspend fun getPersonDetails(
        personId: Long
    ): PersonDetails? {

        val person = personDao.getPersonById(personId)
            ?: return null

        val recognition =
            recognitionDao.getLatestRecognition(personId)
                ?: return null

        val history =
            historyDao.getByRecognitionId(
                recognition.recognitionId
            )

        val conversation =
            conversationDao.getByRecognitionId(
                recognition.recognitionId
            )

        return PersonDetails(
            person = person,
            recognition = recognition,
            history = history,
            conversation = conversation
        )
    }

    suspend fun getRecognitionDetailsForPerson(
        personId: Long
    ): List<RecognitionDetails> {

        val recognitions =
            recognitionDao.getEventsForPerson(personId)

        return recognitions.map { recognition ->

            val history =
                historyDao.getByRecognitionId(
                    recognition.recognitionId
                )

            val conversation =
                conversationDao.getByRecognitionId(
                    recognition.recognitionId
                )

            RecognitionDetails(
                recognition = recognition,
                history = history,
                conversation = conversation
            )
        }
    }
    // will return: Recognition #10 → Aug 27 → location + conversation
    //Recognition #9  → Aug 25 → location only
    //Recognition #8  → Aug 20 → conversation only

    suspend fun updatePerson(
        personId: Long,
        name: String,
        relation: String,
        profileImagePath: String
    ) {
        personDao.updatePersonDetails(
            personId = personId,
            name = name,
            relation = relation,
            profileImagePath = profileImagePath
        )
    }

    suspend fun getPerson(personId: Long): Person? {
        return personDao.getPersonById(personId)
    }

    suspend fun getAllPersons(): List<Person> {
        return personDao.getAllPersons()
    }

    suspend fun getAllFaceEmbeddings(): List<FaceEmbedding> {
        return faceEmbeddingDao.getAllEmbeddings()
    }

    suspend fun getAllVoiceEmbeddings(): List<VoiceEmbedding> {
        return voiceEmbeddingDao.getAllEmbeddings()
    }

    // Query a single person instead of loading every embedding and filtering.
    suspend fun getFaceEmbeddingsForPerson(personId: Long): List<FaceEmbedding> {
        return faceEmbeddingDao.getEmbeddingsForPerson(personId)
    }

    suspend fun getVoiceEmbeddingsForPerson(personId: Long): List<VoiceEmbedding> {
        return voiceEmbeddingDao.getEmbeddingsForPerson(personId)
    }

    suspend fun findExistingPerson(
        name: String,
        relation: String
    ): Person? {
        return personDao.findByNameAndRelation(name, relation)
    }

    /**
     * Deletes the person (their recognitions, conversations, pending items
     * and embeddings cascade in Room) and then the files that Room does not
     * know how to remove: pending audio and the profile image.
     */
    suspend fun deletePerson(person: Person) {

        val audioPaths = db.withTransaction {

            val paths =
                pendingDao.getAudioPathsForPerson(person.personId)

            personDao.delete(person)

            paths
        }

        audioPaths.forEach { path ->
            audioStorage.deleteAudio(path)
        }

        imageStorage.deleteProfileImage(person.personId)

        if (person.profileImagePath.isNotBlank()) {
            imageStorage.deleteProfileImage(person.profileImagePath)
        }
    }


    suspend fun addFaceEmbedding(
        embedding: FaceEmbedding
    ): Long {

        return db.withTransaction {

            // Check the current number of face embeddings for this person.
            val count =
                faceEmbeddingDao.getEmbeddingCount(embedding.personId)

            // If fewer than 3 exist, insert the new embedding.
            if (count < 3) {
                return@withTransaction faceEmbeddingDao.insert(embedding)
            }

            // Three already exist, so find the weakest one.
            val lowestQuality =
                faceEmbeddingDao.getLowestQualityEmbedding(
                    embedding.personId
                ) ?: throw IllegalStateException(
                    "Could not find the lowest-quality face embedding."
                )

            // Keep the existing embedding if the new one isn't better.
            if (embedding.quality <= lowestQuality.quality) {
                return@withTransaction lowestQuality.embeddingId
            }

            // Replace the weakest embedding with the better one.
            faceEmbeddingDao.delete(lowestQuality)

            faceEmbeddingDao.insert(embedding)
        }
    }


    suspend fun addVoiceEmbedding(
        embedding: VoiceEmbedding
    ): Long {

        return db.withTransaction {

            // Check the current number of voice embeddings for this person.
            val count =
                voiceEmbeddingDao.getEmbeddingCount(embedding.personId)

            // If fewer than 3 exist, insert the new embedding.
            if (count < 3) {

                val insertedId = voiceEmbeddingDao.insert(embedding)

                // A person with a voice sample can have conversations recorded.
                personDao.setVoiceEnabled(
                    personId = embedding.personId,
                    enabled = true
                )

                return@withTransaction insertedId
            }

            // Three already exist, so find the weakest one.
            val lowestQuality =
                voiceEmbeddingDao.getLowestQualityEmbedding(
                    embedding.personId
                ) ?: throw IllegalStateException(
                    "Could not find the lowest-quality voice embedding."
                )

            // Keep the existing embedding if the new one isn't better.
            if (embedding.quality <= lowestQuality.quality) {
                return@withTransaction lowestQuality.voiceEmbeddingId
            }

            // Replace the weakest embedding with the better one.
            voiceEmbeddingDao.delete(lowestQuality)

            voiceEmbeddingDao.insert(embedding)
        }
    }


    suspend fun searchPersonsByName(
        name: String
    ): List<Person> {
        return personDao.searchPersonsByName(name)
    }
    // Returns whether voice recording is enabled for this person.
    suspend fun isVoiceEnabled(personId: Long): Boolean {
        return personDao.isVoiceEnabled(personId) ?: false
    }
}
