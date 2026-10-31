package com.example.evoke.database.repository

import androidx.room.withTransaction
import com.example.evoke.database.FaceEmbedding
import com.example.evoke.database.Person
import com.example.evoke.database.PersonDetails
import com.example.evoke.database.RecognitionDetails
import com.example.evoke.database.VoiceEmbedding
import com.example.evoke.database.AppDatabase

class PersonRepository(
    private val db: AppDatabase,
    private val recognitionRepository: RecognitionRepository

) {

    private val personDao = db.personDao()
    private val faceEmbeddingDao = db.faceEmbeddingDao()
    private val voiceEmbeddingDao = db.voiceEmbeddingDao()
    private val recognitionDao = db.recognitionEventDao()
    private val historyDao = db.personHistoryDao()
    private val conversationDao = db.conversationDao()

    suspend fun createPerson(
        person: Person,
        firstFaceEmbedding: FaceEmbedding,
        dateTime: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        rawConversation: String? = null,
        voiceEmbedding: VoiceEmbedding? = null
    ): Long {

        return db.withTransaction {

            val personId = personDao.insert(
                person.copy(
                    voiceEnabled = voiceEmbedding != null
                )
            )

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

            recognitionRepository.createRecognitionInternal(
                personId = personId,
                dateTime = dateTime,
                latitude = latitude,
                longitude = longitude,
                rawConversation = rawConversation
            )

            personId
        }
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

    suspend fun findExistingPerson(
        name: String,
        relation: String
    ): Person? {
        return personDao.findByNameAndRelation(name, relation)
    }

    suspend fun deletePerson(person: Person) {
        personDao.delete(person)
    }

    suspend fun addFaceEmbedding(
        embedding: FaceEmbedding
    ): Long {

        val count =
            faceEmbeddingDao.getEmbeddingCount(embedding.personId)

        // Less than 3 → simply add the new embedding
        if (count < 3) {
            return faceEmbeddingDao.insert(embedding)
        }

        // Already have 3 → find the weakest one
        val lowestQuality =
            faceEmbeddingDao.getLowestQualityEmbedding(
                embedding.personId
            ) ?: return faceEmbeddingDao.insert(embedding)

        // New embedding isn't better → don't store it
        if (embedding.quality <= lowestQuality.quality) {
            return lowestQuality.embeddingId
        }

        // New embedding is better → replace weakest
        return db.withTransaction {
            faceEmbeddingDao.delete(lowestQuality)

            faceEmbeddingDao.insert(embedding)
        }
    }

    suspend fun addVoiceEmbedding(
        embedding: VoiceEmbedding
    ): Long {

        val count =
            voiceEmbeddingDao.getEmbeddingCount(embedding.personId)

        // Less than 3 → simply add the new embedding
        if (count < 3) {
            return voiceEmbeddingDao.insert(embedding)
        }

        // Already have 3 → find the weakest one
        val lowestQuality =
            voiceEmbeddingDao.getLowestQualityEmbedding(
                embedding.personId
            ) ?: return voiceEmbeddingDao.insert(embedding)

        // New embedding isn't better → don't store it
        if (embedding.quality <= lowestQuality.quality) {
            return lowestQuality.voiceEmbeddingId
        }

        // New embedding is better → replace weakest
        return db.withTransaction {
            voiceEmbeddingDao.delete(lowestQuality)

            voiceEmbeddingDao.insert(embedding)
        }
    }

    suspend fun searchPersonsByName(
        name: String
    ): List<Person> {
        return personDao.searchPersonsByName(name)
    }
}