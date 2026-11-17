package com.example.evoke.voice

import android.content.Context
import android.util.Log
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.UserVoiceEmbedding
import com.example.evoke.database.VoiceEmbedding
import com.example.evoke.database.repository.PersonRepository
import com.example.evoke.database.repository.RecognitionRepository
import com.example.evoke.database.repository.UserRepository
import com.example.evoke.storage.AudioStorage
import com.example.evoke.storage.PersonImageStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed class EnrollmentResult {
    /** The sample was processed and offered to the database. */
    data class Saved(
        val speechSeconds: Float,
        val quality: Float,
        /** Similarity to this speaker's earlier samples (null for the first sample). */
        val similarityToExisting: Float?,
        /**
         * False when the speaker already had 3 samples that were all at least
         * this good: the repositories keep only the best 3, so this one was
         * not stored (the voiceprint is still complete and usable).
         */
        val stored: Boolean,
        val totalSamplesForSpeaker: Int
    ) : EnrollmentResult()

    /** Not enough speech in the recording; ask to speak longer. */
    data class TooLittleSpeech(val speechSeconds: Float, val requiredSeconds: Float) : EnrollmentResult()

    /** The sample doesn't sound like this speaker's earlier samples. */
    data class DoesNotMatchExisting(val similarity: Float) : EnrollmentResult()

    /**
     * A "partner" sample that sounds like the patient. [voiceprint] is the
     * sample's voiceprint, so it can still be kept when the caregiver confirms
     * it is a different person.
     */
    data class SoundsLikePatient(
        val similarity: Float,
        val voiceprint: VoiceprintResult.Ready? = null
    ) : EnrollmentResult()

    data class Failed(val message: String) : EnrollmentResult()
}

/**
 * A voiceprint made from a sample but NOT saved yet.
 *
 * Used when the owner is not in the database yet, because the frontend
 * creates them together with their voice sample in one transaction:
 * - patient: UserRepository.createUser(user, contacts, listOf(print.toUserVoiceEmbedding()))
 * - partner: PersonRepository.createPerson(..., voiceEmbedding = print.toVoiceEmbedding())
 *   (createPerson fills in the real personId and sets voiceEnabled)
 *
 * [embedding] can also be passed straight to ConversationPipeline.filter() as the
 * partner's voiceprint, to filter a conversation recorded before the person exists.
 */
sealed class VoiceprintResult {
    class Ready(
        val embedding: FloatArray,
        val speechSeconds: Float,
        val quality: Float
    ) : VoiceprintResult() {

        fun toUserVoiceEmbedding(): UserVoiceEmbedding =
            UserVoiceEmbedding(
                embedding = EmbeddingMath.toBytes(embedding),
                type = VoiceConfig.EMBEDDING_TYPE,
                quality = quality
            )

        /** personId 0 = placeholder; PersonRepository.createPerson replaces it. */
        fun toVoiceEmbedding(personId: Long = 0): VoiceEmbedding =
            VoiceEmbedding(
                personId = personId,
                embedding = EmbeddingMath.toBytes(embedding),
                type = VoiceConfig.EMBEDDING_TYPE,
                quality = quality
            )
    }

    /** Not usable; [reason] is TooLittleSpeech, DoesNotMatchExisting, SoundsLikePatient or Failed. */
    class Rejected(val reason: EnrollmentResult) : VoiceprintResult()
}

/**
 * Steps:
 *   Patient Voice Sample → (Silero VAD) → CAM++ → user_voice_embeddings
 *   Partner Voice Sample → (Silero VAD) → CAM++ → voice_embeddings (personId)
 *
 * VAD is run first so silence / background noise doesn't go into the voiceprint.
 * Saving goes through the frontend repositories, which keep the best 3 samples
 * per speaker (by quality) and, for a partner, set Person.voiceEnabled.
 *
 * None of these functions delete [sampleWav]; the caller does that.
 */
class VoiceEnrollmentManager(
    context: Context,
    private val db: AppDatabase
) {
    private val appContext = context.applicationContext

    private val audioStorage = AudioStorage(appContext)
    private val imageStorage = PersonImageStorage(appContext)

    private val userRepository = UserRepository(db, audioStorage, imageStorage)

    private val personRepository = PersonRepository(
        db = db,
        recognitionRepository = RecognitionRepository(db, audioStorage),
        audioStorage = audioStorage,
        imageStorage = imageStorage
    )

    // ── save to the database ─────────────────────────────────────────────────

    /** Patient = the app user (single user). Adds a sample to user_voice_embeddings. */
    suspend fun enrollPatient(sampleWav: File, force: Boolean = false): EnrollmentResult =
        withContext(Dispatchers.Default) {
            try {
                val print = when (val r = createPatientVoiceprint(sampleWav, force)) {
                    is VoiceprintResult.Rejected -> return@withContext r.reason
                    is VoiceprintResult.Ready -> r
                }
                val similarity = similarityTo(print.embedding, loadPatientEmbeddings(db))

                val idsBefore = db.userVoiceEmbeddingDao().getAllEmbeddings()
                    .map { it.userVoiceEmbeddingId }.toSet()
                val id = userRepository.addVoiceEmbedding(print.toUserVoiceEmbedding())

                EnrollmentResult.Saved(
                    speechSeconds = print.speechSeconds,
                    quality = print.quality,
                    similarityToExisting = similarity,
                    stored = id !in idsBefore,
                    totalSamplesForSpeaker = loadPatientEmbeddings(db).size
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EnrollmentResult.Failed(e.message ?: e.javaClass.simpleName)
            }
        }

    /**
     * Partner = a person already in the `persons` table (the one being talked to).
     * Adds a sample to voice_embeddings; PersonRepository sets voiceEnabled.
     */
    suspend fun enrollPartner(personId: Long, sampleWav: File, force: Boolean = false): EnrollmentResult =
        withContext(Dispatchers.Default) {
            try {
                personRepository.getPerson(personId)
                    ?: return@withContext EnrollmentResult.Failed("Person $personId not found")

                val print = when (val r = createPartnerVoiceprint(sampleWav, personId, force)) {
                    is VoiceprintResult.Rejected -> return@withContext r.reason
                    is VoiceprintResult.Ready -> r
                }
                val similarity = similarityTo(print.embedding, loadPartnerEmbeddings(db, personId))

                val idsBefore = personRepository.getVoiceEmbeddingsForPerson(personId)
                    .map { it.voiceEmbeddingId }.toSet()
                val id = personRepository.addVoiceEmbedding(print.toVoiceEmbedding(personId))

                EnrollmentResult.Saved(
                    speechSeconds = print.speechSeconds,
                    quality = print.quality,
                    similarityToExisting = similarity,
                    stored = id !in idsBefore,
                    totalSamplesForSpeaker = loadPartnerEmbeddings(db, personId).size
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EnrollmentResult.Failed(e.message ?: e.javaClass.simpleName)
            }
        }

    // ── voiceprint only (nothing saved) ──────────────────────────────────────

    /**
     * Patient voiceprint for UserRepository.createUser (needs 1–3 of them).
     * If patient samples already exist, the new one must sound like them.
     */
    suspend fun createPatientVoiceprint(sampleWav: File, force: Boolean = false): VoiceprintResult =
        withContext(Dispatchers.Default) {
            try {
                val print = extract(sampleWav)
                if (print !is VoiceprintResult.Ready) return@withContext print

                val similarity = similarityTo(print.embedding, loadPatientEmbeddings(db))
                if (!force && similarity != null && similarity < VoiceConfig.ENROLL_CONSISTENCY_THRESHOLD) {
                    return@withContext VoiceprintResult.Rejected(
                        EnrollmentResult.DoesNotMatchExisting(similarity)
                    )
                }
                print
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                VoiceprintResult.Rejected(EnrollmentResult.Failed(e.message ?: e.javaClass.simpleName))
            }
        }

    /**
     * Partner voiceprint. With [personId] = null (person not saved yet) it is
     * meant for PersonRepository.createPerson(voiceEmbedding = ...); with a
     * personId the new sample must also sound like that person's earlier ones.
     *
     * Refused (unless [force]) when it sounds like the patient.
     */
    suspend fun createPartnerVoiceprint(
        sampleWav: File,
        personId: Long? = null,
        force: Boolean = false
    ): VoiceprintResult =
        withContext(Dispatchers.Default) {
            try {
                val print = extract(sampleWav)
                if (print !is VoiceprintResult.Ready) return@withContext print

                if (!force) {
                    // Guard: is this actually the patient speaking?
                    val check = checkAgainstPatient(print.embedding)
                    if (check != null && check.isPatient) {
                        return@withContext VoiceprintResult.Rejected(
                            // The voiceprint is included so the caller can keep it
                            // anyway when the caregiver knows it is a different person.
                            EnrollmentResult.SoundsLikePatient(check.score, voiceprint = print)
                        )
                    }

                    if (personId != null) {
                        val similarity = similarityTo(print.embedding, loadPartnerEmbeddings(db, personId))
                        if (similarity != null && similarity < VoiceConfig.ENROLL_CONSISTENCY_THRESHOLD) {
                            return@withContext VoiceprintResult.Rejected(
                                EnrollmentResult.DoesNotMatchExisting(similarity)
                            )
                        }
                    }
                }
                print
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                VoiceprintResult.Rejected(EnrollmentResult.Failed(e.message ?: e.javaClass.simpleName))
            }
        }

    /**
     * Saves a partner voiceprint that was refused as SoundsLikePatient, after the
     * caregiver confirmed it is a different person ("Keep this sample").
     */
    suspend fun savePartnerVoiceprint(personId: Long, print: VoiceprintResult.Ready): EnrollmentResult =
        withContext(Dispatchers.Default) {
            try {
                val idsBefore = personRepository.getVoiceEmbeddingsForPerson(personId)
                    .map { it.voiceEmbeddingId }.toSet()
                val id = personRepository.addVoiceEmbedding(print.toVoiceEmbedding(personId))
                EnrollmentResult.Saved(
                    speechSeconds = print.speechSeconds,
                    quality = print.quality,
                    similarityToExisting = null,
                    stored = id !in idsBefore,
                    totalSamplesForSpeaker = loadPartnerEmbeddings(db, personId).size
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                EnrollmentResult.Failed(e.message ?: e.javaClass.simpleName)
            }
        }

    // ── patient-vs-visitor check ─────────────────────────────────────────────

    private class PatientCheck(val score: Float, val patientSelfScore: Float?, val isPatient: Boolean)

    /**
     * Does a visitor sample sound like the patient?
     *
     * Compares with the patient's AVERAGE voiceprint (not the single closest
     * sample, which one unusually close recording can push up), and calibrates
     * against how alike the patient's own samples are: a different person
     * should not score about as high as the patient's own samples score against
     * each other. Same phone, same room and the same script push scores of two
     * different people up, so a fixed cut-off alone refused real visitors.
     *
     * isPatient = score >= PARTNER_PATIENT_CONFLICT_THRESHOLD
     *             and score >= patientSelfScore - PATIENT_SELF_MARGIN
     *             (patientSelfScore needs 2+ patient samples)
     *
     * Scores are logged (tag "VoiceEnrollment") so the values can be tuned.
     */
    private suspend fun checkAgainstPatient(embedding: FloatArray): PatientCheck? {
        val patient = loadPatientEmbeddings(db)
        if (patient.isEmpty()) return null

        val score = EmbeddingMath.cosine(embedding, EmbeddingMath.centroid(patient))

        // Leave-one-out: each patient sample against the average of the others.
        val selfScore = if (patient.size >= 2) {
            patient.indices.map { i ->
                val others = patient.filterIndexed { j, _ -> j != i }
                EmbeddingMath.cosine(patient[i], EmbeddingMath.centroid(others))
            }.average().toFloat()
        } else {
            null
        }

        val isPatient = score >= VoiceConfig.PARTNER_PATIENT_CONFLICT_THRESHOLD &&
            (selfScore == null || score >= selfScore - VoiceConfig.PATIENT_SELF_MARGIN)

        Log.d(
            TAG,
            "Visitor vs patient: score=%.3f, patient self-similarity=%s, samples=%d -> %s".format(
                score,
                selfScore?.let { "%.3f".format(it) } ?: "n/a",
                patient.size,
                if (isPatient) "SOUNDS LIKE PATIENT" else "different person"
            )
        )
        return PatientCheck(score, selfScore, isPatient)
    }

    // ── shared extraction ──────────────────────────────────────────────────────

    private fun similarityTo(embedding: FloatArray, references: List<FloatArray>): Float? =
        references.takeIf { it.isNotEmpty() }?.let { EmbeddingMath.bestScore(embedding, it) }

    private fun extract(sampleWav: File): VoiceprintResult {
        val samples = WavReader(sampleWav).use { reader ->
            require(reader.sampleRate == VoiceConfig.SAMPLE_RATE) {
                "Audio must be ${VoiceConfig.SAMPLE_RATE} Hz (got ${reader.sampleRate})"
            }
            reader.readAll()
        }
        val regions = SpeechSegmenter(appContext).detect(samples)

        // Keep only the speech parts.
        val speechLength = regions.sumOf { it.lengthSamples }.toInt()
        val speech = FloatArray(speechLength)
        var pos = 0
        for (r in regions) {
            val s = r.startSample.toInt().coerceIn(0, samples.size)
            val e = r.endSample.toInt().coerceIn(s, samples.size)
            System.arraycopy(samples, s, speech, pos, e - s)
            pos += e - s
        }
        val speechSeconds = pos.toFloat() / VoiceConfig.SAMPLE_RATE

        if (speechSeconds < VoiceConfig.MIN_ENROLL_SPEECH_SEC) {
            return VoiceprintResult.Rejected(
                EnrollmentResult.TooLittleSpeech(speechSeconds, VoiceConfig.MIN_ENROLL_SPEECH_SEC)
            )
        }

        val embedding = SpeakerEmbedder(appContext).embed(speech.copyOf(pos))
            ?: return VoiceprintResult.Rejected(EnrollmentResult.Failed("Could not compute a voice embedding"))

        // Quality = how much speech we had (more speech → steadier voiceprint).
        // Used by the repositories to keep the best 3 samples per speaker.
        val quality = (speechSeconds / VoiceConfig.TARGET_ENROLL_SPEECH_SEC).coerceAtMost(1f)

        return VoiceprintResult.Ready(embedding, speechSeconds, quality)
    }

    companion object {
        private const val TAG = "VoiceEnrollment"

        /** Patient voiceprints made with the current model. */
        suspend fun loadPatientEmbeddings(db: AppDatabase): List<FloatArray> =
            db.userVoiceEmbeddingDao().getAllEmbeddings()
                .filter { it.type == VoiceConfig.EMBEDDING_TYPE }
                .map { EmbeddingMath.fromBytes(it.embedding) }

        /** A partner's voiceprints made with the current model. */
        suspend fun loadPartnerEmbeddings(db: AppDatabase, personId: Long): List<FloatArray> =
            db.voiceEmbeddingDao().getEmbeddingsForPerson(personId)
                .filter { it.type == VoiceConfig.EMBEDDING_TYPE }
                .map { EmbeddingMath.fromBytes(it.embedding) }
    }
}
