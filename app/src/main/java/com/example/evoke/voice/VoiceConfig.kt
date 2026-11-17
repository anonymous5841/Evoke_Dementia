package com.example.evoke.voice

/**
 * All tunable values for the voice pipeline in one place.
 *
 * Pipeline:
 *   Patient sample  ──► Silero VAD ──► CAM++ ──► user_voice_embeddings
 *   Partner sample  ──► Silero VAD ──► CAM++ ──► voice_embeddings (per person)
 *   Conversation    ──► Silero VAD ──► segments ──► CAM++ ──► compare
 *                       ──► keep PATIENT / PARTNER, remove UNKNOWN ──► filtered WAV
 *                       ──► AudioStorage (files/audio/<uuid>.wav)
 *                       ──► pending_conversations (WAITING_TRANSCRIPTION),
 *                           transcribed and summarised online later
 *
 * Conversations are filtered WHILE they are recorded (LiveConversationFilter),
 * so after Stop only the last speech region and the file writing remain.
 */
object VoiceConfig {

    // ── Audio format (both models expect 16 kHz mono) ─────────────────────────
    const val SAMPLE_RATE = 16_000

    // ── Model files (must be in app/src/main/assets/, NOT in a sub-folder) ────
    const val VAD_MODEL_ASSET = "silero_vad.onnx"

    /**
     * Speaker-embedding model: 3D-Speaker CAM++ trained on VoxCeleb, 16 kHz,
     * from sherpa-onnx's "speaker-recongition-models" release
     * (sha256 357a834f702b80161e5b981182c038e18553c1f2ca752ed6cec2052365d4129b).
     * 7.2 M parameters, several times faster than ECAPA-TDNN with a lower
     * published error rate.
     */
    const val SPEAKER_MODEL_ASSET = "3dspeaker_speech_campplus_sv_en_voxceleb_16k.onnx"

    /**
     * Saved in the `type` column of user_voice_embeddings / voice_embeddings.
     * Embeddings from a different model are not comparable, so anything with
     * another type is ignored. Change this if you ever change the model.
     */
    const val EMBEDDING_TYPE = "campplus_3dspeaker_vox"

    /**
     * CPU threads for the speaker model: one less than the phone's cores,
     * between 2 and 4. One core always stays free for the microphone and the
     * UI (filtering runs while recording); more than 4 rarely helps on phones
     * and only makes them warm.
     */
    fun speakerThreads(): Int =
        (Runtime.getRuntime().availableProcessors() - 1).coerceIn(2, 4)

    // ── Silero VAD ────────────────────────────────────────────────────────────
    const val VAD_WINDOW_SIZE = 512              // samples per VAD step (32 ms)
    const val VAD_THRESHOLD = 0.5f               // speech probability
    const val VAD_MIN_SILENCE_SEC = 0.30f        // pause that ends a segment
    const val VAD_MIN_SPEECH_SEC = 0.25f         // ignore shorter blips
    const val VAD_MAX_SPEECH_SEC = 15.0f         // long speech is cut here

    // ── Speaker matching (cosine similarity, range -1..1) ─────────────────────
    /**
     * A segment must score at least this against the patient or the partner,
     * otherwise it is UNKNOWN and removed.
     * Every analysed piece's scores are in FilterResult.pieces (patientScore /
     * partnerScore) — log them to tune this. 0.45 was first tuned for
     * ECAPA-TDNN; CAM++ scores spread differently, so re-check it (and the two
     * enrollment thresholds below) with real patient / partner / stranger
     * recordings.
     */
    const val SPEAKER_MATCH_THRESHOLD = 0.45f

    /** Speech shorter than this is too short for a reliable embedding. */
    const val MIN_EMBED_SEC = 1.0f

    /** Segments longer than MAX_WINDOW_SEC are split into ~TARGET_WINDOW_SEC
     *  pieces, so a turn change without a pause is still caught. */
    const val TARGET_WINDOW_SEC = 2.0f
    const val MAX_WINDOW_SEC = 3.0f

    /** A short piece (< MIN_EMBED_SEC) takes the label of a neighbouring
     *  verified piece if it is at most this far away; otherwise UNKNOWN. */
    const val INHERIT_MAX_GAP_SEC = 1.0f

    /** Neighbouring kept pieces of the same speaker closer than this are
     *  merged into one segment. */
    const val MERGE_GAP_SEC = 0.5f

    /** Silence inserted between kept segments in the filtered WAV. */
    const val FILTERED_GAP_MS = 400

    // ── Enrollment (voice samples) ────────────────────────────────────────────
    /** Minimum speech (after VAD removes silence) for a usable voice sample. */
    const val MIN_ENROLL_SPEECH_SEC = 5.0f

    /** Speech length that gives quality 1.0. Ask people to talk ~15 s. */
    const val TARGET_ENROLL_SPEECH_SEC = 12.0f

    /** A new sample of an already-enrolled speaker must match their previous
     *  samples at least this much (catches the wrong person speaking). */
    const val ENROLL_CONSISTENCY_THRESHOLD = 0.40f

    /** A partner sample can only count as "the patient speaking" when it scores
     *  at least this against the patient's average voiceprint (see
     *  VoiceEnrollmentManager.checkAgainstPatient). */
    const val PARTNER_PATIENT_CONFLICT_THRESHOLD = 0.60f

    /** ...and when it scores within this margin of how alike the patient's own
     *  samples are. A different person (even on the same phone, in the same
     *  room, reading the same script) stays below that level. The caregiver can
     *  still keep a refused sample ("Keep this sample"). */
    const val PATIENT_SELF_MARGIN = 0.05f

    // ── Recording limits ──────────────────────────────────────────────────────
    const val MAX_ENROLL_RECORDING_MS = 60_000L          // 1 minute
    const val MAX_CONVERSATION_RECORDING_MS = 30 * 60_000L // 30 minutes
}
