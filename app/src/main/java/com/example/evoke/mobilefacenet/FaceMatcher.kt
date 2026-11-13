package com.example.evoke.mobilefacenet

import android.util.Log
import kotlin.math.sqrt

/**
 * One enrolled person: between 1 and FaceConfig.MAX_FACES_PER_PERSON reference embeddings.
 * Build these with FaceTemplateStore.loadReferences().
 */
data class PersonReference(
    val personId: String,
    val embeddings: List<FloatArray>
)

/**
 * Result returned after comparing a captured face with enrolled people.
 */
data class FaceMatchResult(
    val isRecognized: Boolean,
    val personId: String?,
    val similarity: Float,
    val threshold: Float
)

object FaceMatcher {

    private const val EMBEDDING_SIZE = 128

    private const val TAG = "FaceMatch"
    private const val DEBUG_LOGS = true   // set false before release

    // Temporary hardcoded reference embeddings for testing
    val testReferences = listOf(

        PersonReference(
            personId = "azeema",
            embeddings = listOf(
                floatArrayOf(
                    0.06606201f, 0.028366478f, -0.048771944f, -0.07990012f, 0.03116001f, -0.13045551f, -0.02135561f, 0.039035585f,
                    0.07366676f, 0.0604629f, -0.13106649f, -0.10120686f, -0.1481217f, 0.2135344f, 0.062505014f, -0.06553508f,
                    -0.089047626f, -0.09935224f, -0.027400628f, 0.017941177f,
                    0.08838641f, 0.0062728077f, 0.029301634f, -0.16425522f, -0.04733604f, 9.90746E-4f, -0.16685514f, -0.0034222975f,
                    -0.09424173f, 0.117069565f, 0.05597209f, 0.07213369f, 0.11706488f, -0.05180963f, -0.052976355f, -0.041026156f,
                    -0.08936679f, -0.08089077f, -0.007518481f, -0.07612076f,
                    0.017165394f, 0.029251143f, -0.077631116f, -0.03854788f, 0.021300688f, 0.045223065f, -0.092421636f, 0.034584768f,
                    -0.035509232f, 0.11206883f, 0.00960669f, 0.009467659f, 0.01051565f, -0.19107349f, 0.046405926f, -0.028130163f,
                    0.04310592f, 0.18549868f, -0.0077492553f, -0.16206028f,
                    -0.08949124f, -0.15955655f, 0.11178313f, 0.011938595f, 7.0882303E-4f, -0.005570992f, -0.09257324f, -0.038543645f,
                    0.020674929f, 0.02150089f, -0.022524916f, 0.10646099f, 0.020449217f, -0.1554836f, -0.12281027f, -0.035144746f,
                    0.0010335703f, -0.010530968f, 0.0045623444f, 0.16612054f,
                    0.17573981f, 0.23270567f, -0.023245148f, -0.10114118f, 0.03083821f, 0.013787837f, -0.0684131f, 0.087545455f,
                    -0.010248313f, -0.11448344f, -0.056902215f, 0.20479365f, -0.059429243f, -0.011972786f, -0.14980088f, -0.0078663165f,
                    0.016299251f, -0.042573307f, -0.050152294f, 6.9824414E-4f,
                    0.01588368f, 0.10600935f, 0.13415238f, 0.08774544f, 0.06250122f, -0.027021952f, 0.05246375f, -0.017757898f,
                    0.054981515f, 0.11686002f, 0.07995528f, -0.1812876f, -0.0045969496f, -0.10428161f, -0.013620768f, 0.06528834f,
                    0.089749664f, 0.035315983f, -0.08625466f, -0.008176661f,
                    0.12375436f, 0.00928728f, 0.10202922f, 0.08995321f, 0.024167003f, 0.13596329f, 0.1699741f, 0.09538965f
                ),
                floatArrayOf(
                    0.04053306f, 0.099928185f, -0.08085414f, -0.08610121f, 0.0018753031f, -0.1730493f, -0.029176855f, 0.025342911f,
                    0.1088022f, 0.106933564f, -0.10481854f, -0.03662156f, -0.13869384f, 0.109409004f, 0.053373903f, -0.05215853f,
                    -0.1265011f, -0.1528653f, 0.012589761f, 0.057775933f,
                    0.08729779f, 0.014677643f, 0.052012607f, -0.20841792f, -0.034258902f, 0.028289378f, -0.07990184f, -0.016018094f,
                    -0.07909429f, 0.16593082f, 0.03446569f, 0.08130279f, 0.07354274f, 0.027798988f, -0.017824957f, -0.01626405f,
                    -0.11520552f, -0.09274731f, -0.028409386f, -0.034611847f,
                    0.0598264f, 0.07125405f, -0.10389994f, -0.038959518f, 0.061006963f, 0.058935784f, -0.06115953f, -0.046226095f,
                    -0.049622837f, 0.14516708f, -0.046458088f, -0.008621189f, 0.006427658f, -0.19529562f, 0.019534634f, 0.056223124f,
                    0.027141565f, 0.12495839f, 0.0026461766f, -0.056214802f,
                    0.0062496634f, -0.15443535f, 0.13378856f, -0.03609813f, -0.09536543f, 0.029734643f, -0.1250581f, -0.018252032f,
                    -0.008031184f, -0.027390342f, -0.07426418f, 0.029212587f, 0.041521937f, -0.078322746f, -0.05569518f, 0.007630519f,
                    0.008405686f, -0.07190205f, -0.01826617f, 0.14923774f,
                    0.2044147f, 0.23361488f, -0.083816156f, -0.10927725f, -0.007136256f, 0.00744151f, -0.06336343f, 0.06494639f,
                    -0.07351227f, -0.13180023f, -0.03936508f, 0.14796278f, 0.038442764f, -0.019695416f, -0.15893921f, 0.006895233f,
                    0.06524489f, -0.009519723f, 0.045820452f, 0.093744375f,
                    -0.01713739f, 0.062113974f, 0.16748162f, 0.016897019f, 0.15764152f, -0.026974501f, 0.10697005f, -0.078305624f,
                    0.08387878f, 0.05181129f, 0.032404006f, -0.15921779f, -0.013516376f, -0.06610958f, -0.056536373f, 0.046087407f,
                    0.077549174f, 0.058675367f, -0.02482733f, -0.01058448f,
                    0.07387373f, 0.05697029f, 0.15133226f, 0.09184011f, 0.11505955f, 0.13324532f, 0.15067254f, 0.13753654f
                ),
                floatArrayOf(
                    0.01910374f, 0.08880404f, -0.034116987f, -0.042412587f, -0.01830596f, -0.22546446f, -0.095993705f, 0.048531245f, 0.11467887f, 0.095010236f, -0.0823165f, -0.061299715f, -0.18575777f, 0.13910253f, 0.07112445f, -0.051737025f, -0.10260204f, -0.15410791f, 0.04005065f, 0.020661611f,
                    0.066746615f, -0.06988176f, 0.03428634f, -0.11181598f, -0.027076546f, 0.018634016f, -0.053828925f, 0.063154034f, -0.1419748f, 0.15040313f, 0.085977525f, 0.12623172f, 0.02072342f, -0.031090498f, 0.024557184f, -0.0053630164f, -0.050203227f, -0.07868283f, -0.050328087f, 0.034096297f,
                    0.02680913f, 0.010537582f, -0.1294267f, 0.012592609f, 0.10389777f, 0.074551426f, -0.07426986f, -0.04147254f, -0.05454351f, 0.02635616f, -0.06859111f, -0.02064448f, 0.061579917f, -0.20207155f, 0.059839807f, 0.0182372f, 0.036663234f, 0.12535764f, -0.008561512f, -0.027193392f,
                    0.0084024025f, -0.11063512f, 0.121573694f, 0.0069016945f, -0.11166123f, 0.054488983f, -0.09766167f, -0.011977963f, 0.007968826f, 0.0764361f, -0.12513551f, 0.0336326f, 0.012098977f, -0.076490186f, -0.099786475f, 0.025869466f, 0.003861355f, -0.05269485f, -0.026371859f, 0.16123468f,
                    0.20631877f, 0.24426793f, -0.095995985f, -0.10996581f, -0.012839599f, -0.021118548f, -0.08889272f, -0.021831717f, -0.10023367f, -0.13490507f, -0.10409815f, 0.13000007f, 0.07690655f, 0.074746616f, -0.09858978f, -0.027904812f, 0.05265015f, -0.013768253f, 0.058403816f, 0.04210701f,
                    -0.011960435f, 0.10260592f, 0.15824959f, -0.04010858f, 0.17046338f, -0.03428259f, 0.09402807f, -0.035737094f, 0.1174123f, 0.067732975f, 0.026881844f, -0.1737204f, -0.0023657088f, 0.007433287f, -0.08893932f, 0.005146309f, 0.054459766f, 0.03723467f, 0.024119372f, -0.01827768f,
                    0.06779894f, 0.021645667f, 0.13574384f, 0.0046823067f, 0.13425238f, 0.10782241f, 0.10711534f, 0.15100908f
                )
            )
        )
    )

    /**
     * Compare the captured embedding against every enrolled person.
     *
     * 1. Cosine similarity with each of the person's references.
     * 2. Average those similarities per person (same rule the Colab calibration used).
     * 3. Take the person with the highest average.
     * 4. Recognised only if that average reaches the threshold.
     *
     * An empty gallery is not an error: nobody can be recognised, so the result is "not recognised".
     */
    fun identify(
        queryEmbedding: FloatArray,
        references: List<PersonReference>,
        threshold: Float = FaceConfig.RECOGNITION_THRESHOLD
    ): FaceMatchResult {

        require(queryEmbedding.size == EMBEDDING_SIZE) {
            "Query embedding must contain $EMBEDDING_SIZE values."
        }

        var bestPersonId: String? = null
        var bestScore = -1f


// inside identify(), replacing the for-loop:
        val scored = mutableListOf<Triple<String, List<Float>, Float>>()

        for (person in references) {
            val valid = person.embeddings.filter { it.size == EMBEDDING_SIZE }
            if (valid.isEmpty()) continue

            val scores = valid.map { cosineSimilarity(queryEmbedding, it) }
            val average = scores.sum() / scores.size
            scored.add(Triple(person.personId, scores, average))

            if (average > bestScore) {
                bestScore = average
                bestPersonId = person.personId
            }
        }

        if (DEBUG_LOGS) {
            Log.d(TAG, "gallery=${references.size} people, threshold=$threshold")
            scored.sortedByDescending { it.third }.take(5).forEach { (id, scores, avg) ->
                val per = scores.joinToString(" + ") { "%.4f".format(it) }
                Log.d(TAG, "  $id: ($per) / ${scores.size} = %.4f".format(avg))
            }
        }
        if (bestPersonId == null) {
            return FaceMatchResult(
                isRecognized = false,
                personId = null,
                similarity = 0f,
                threshold = threshold
            )
        }

        val recognised = bestScore >= threshold
        return FaceMatchResult(
            isRecognized = recognised,
            personId = if (recognised) bestPersonId else null,
            similarity = bestScore,
            threshold = threshold
        )
    }

    /**
     * Enrolment check: does this new face already look like someone ELSE?
     *
     * Uses a threshold slightly BELOW the recognition threshold, so near-collisions are flagged too.
     * If isRecognized is true, show a warning ("This face looks like <personId>") and let the user
     * decide, because after enrolment this person could be recognised as the other one.
     */
    fun findLookalike(
        embedding: FloatArray,
        references: List<PersonReference>,
        excludePersonId: String?,
        threshold: Float = FaceConfig.RECOGNITION_THRESHOLD - FaceConfig.DUPLICATE_WARNING_MARGIN
    ): FaceMatchResult =
        identify(
            queryEmbedding = embedding,
            references = references.filter { it.personId != excludePersonId },
            threshold = threshold
        )

    /**
     * Cosine similarity. Embeddings are already L2-normalised by FaceEmbedder, so this equals the dot
     * product; the norms are kept as a safety net.
     */
    private fun cosineSimilarity(first: FloatArray, second: FloatArray): Float {
        var dotProduct = 0f
        var firstNorm = 0f
        var secondNorm = 0f

        for (i in first.indices) {
            dotProduct += first[i] * second[i]
            firstNorm += first[i] * first[i]
            secondNorm += second[i] * second[i]
        }

        val denominator = sqrt(firstNorm) * sqrt(secondNorm)
        return if (denominator == 0f) 0f else dotProduct / denominator
    }
}
