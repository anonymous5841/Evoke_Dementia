package com.example.evoke.mobilefacenet

/**
 * The numbers that must stay identical between enrolment and recognition.
 * Keep this in one place so the two flows can never drift apart.
 */
object FaceConfig {

    /**
    Target false accepts	Threshold	Enrolled people missed	Wrong person accepted
    1%	0.510	2.1%	0
    0.5%	0.531	2.9%	0
    0.1%	0.567	5.3%	0
     */
    const val RECOGNITION_THRESHOLD = 0.567f

    /**
     * Enrolment warns when a new face scores within this margin of the recognition threshold against
     * someone else, because that person could be recognised as the other one later.
     */
    const val DUPLICATE_WARNING_MARGIN = 0.05f

    const val MAX_PEOPLE = 100
    const val MAX_FACES_PER_PERSON = 3

    /**
     * Identify the model and preprocessing that produced stored embeddings. Change these whenever the
     * model file, the alignment, flip averaging or any preprocessing changes: stored embeddings with a
     * different tag are ignored by matching until FaceTemplateStore.reembedAll() has refreshed them.
     */
    const val MODEL_ID = "mobilefacenet-sirius-v1"
    const val PIPELINE_VERSION = "mlkit-align4-flip-v1"
}
