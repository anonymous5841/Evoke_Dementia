package com.example.evoke.mobilefacenet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Persistent storage for enrolled faces (up to FaceConfig.MAX_PEOPLE people, up to
 * FaceConfig.MAX_FACES_PER_PERSON faces each).
 *
 * Keyed by YOUR person id, so it works with whatever record store the app already uses.
 *
 * For every face it saves:
 *  - the 128-d embedding (Base64 of the float bytes, exact round trip),
 *  - the aligned 112x112 crop as a PNG, so every embedding can be regenerated after a model change
 *    without asking anyone to register again,
 *  - the model id and pipeline version that produced the embedding.
 *
 * Files live in the app-private folder (filesDir/face_templates). They are biometric data:
 *  - exclude them from Android auto-backup unless you deliberately want them backed up, and
 *  - if they ever join your encrypted Drive backup, encrypt them like the rest.
 */
class FaceTemplateStore(context: Context) {

    class StoredFace(
        val faceId: String,
        val personId: String,
        val embedding: FloatArray,
        val modelId: String,
        val pipelineVersion: String,
        val cropFileName: String,
        val createdAtMillis: Long
    )

    sealed class AddResult {
        class Added(val faceId: String) : AddResult()
        object PersonLimitReached : AddResult()
        object FaceLimitReached : AddResult()
    }

    private val rootDir = File(context.filesDir, "face_templates").apply { mkdirs() }
    private val cropsDir = File(rootDir, "crops").apply { mkdirs() }
    private val indexFile = AtomicFile(File(rootDir, "index.json"))
    private val lock = Any()

    // ---------------------------------------------------------------------------------------------
    // Reading
    // ---------------------------------------------------------------------------------------------

    /** References for FaceMatcher. Only faces made by the CURRENT model and pipeline are returned. */
    fun loadReferences(): List<PersonReference> = synchronized(lock) {
        readAll()
            .filter { isCurrent(it) }
            .groupBy { it.personId }
            .map { (personId, faces) -> PersonReference(personId, faces.map { it.embedding }) }
    }

    fun facesOf(personId: String): List<StoredFace> = synchronized(lock) {
        readAll().filter { it.personId == personId }
    }

    fun personCount(): Int = synchronized(lock) {
        readAll().map { it.personId }.distinct().size
    }

    /** Faces whose embeddings came from a different model or pipeline and need reembedAll(). */
    fun staleFaces(): List<StoredFace> = synchronized(lock) {
        readAll().filterNot { isCurrent(it) }
    }

    fun loadCrop(face: StoredFace): Bitmap? =
        BitmapFactory.decodeFile(File(cropsDir, face.cropFileName).path)

    // ---------------------------------------------------------------------------------------------
    // Writing
    // ---------------------------------------------------------------------------------------------

    fun addFace(personId: String, embedding: FloatArray, alignedFace: Bitmap): AddResult =
        synchronized(lock) { addFaceLocked(personId, embedding, alignedFace) }

    private fun addFaceLocked(personId: String, embedding: FloatArray, alignedFace: Bitmap): AddResult {
        val all = readAll()

        val facesOfPerson = all.count { it.personId == personId }
        if (facesOfPerson == 0 && all.map { it.personId }.distinct().size >= FaceConfig.MAX_PEOPLE) {
            return AddResult.PersonLimitReached
        }
        if (facesOfPerson >= FaceConfig.MAX_FACES_PER_PERSON) {
            return AddResult.FaceLimitReached
        }

        val faceId = UUID.randomUUID().toString()
        val cropName = "$faceId.png"
        val cropFile = File(cropsDir, cropName)

        cropFile.outputStream().use { alignedFace.compress(Bitmap.CompressFormat.PNG, 100, it) }

        all.add(
            StoredFace(
                faceId = faceId,
                personId = personId,
                embedding = embedding,
                modelId = FaceConfig.MODEL_ID,
                pipelineVersion = FaceConfig.PIPELINE_VERSION,
                cropFileName = cropName,
                createdAtMillis = System.currentTimeMillis()
            )
        )

        try {
            writeAll(all)
        } catch (e: Exception) {
            cropFile.delete()   // do not leave an orphan crop behind
            throw e
        }
        return AddResult.Added(faceId)
    }

    fun removeFace(faceId: String) = synchronized(lock) {
        val all = readAll()
        val target = all.firstOrNull { it.faceId == faceId } ?: return@synchronized
        all.remove(target)
        writeAll(all)
        File(cropsDir, target.cropFileName).delete()
    }

    fun removePerson(personId: String) = synchronized(lock) {
        val all = readAll()
        val removed = all.filter { it.personId == personId }
        if (removed.isEmpty()) return@synchronized
        all.removeAll(removed)
        writeAll(all)
        removed.forEach { File(cropsDir, it.cropFileName).delete() }
    }

    /**
     * Re-embeds every stored face from its saved aligned crop and tags it with the current
     * FaceConfig.MODEL_ID / PIPELINE_VERSION. Call after switching model or changing the pipeline:
     *
     *     store.reembedAll { crop -> pipeline.embedAligned(crop) }
     *
     * Returns how many faces were updated. Faces whose crop file is missing are left untouched
     * (and stay ignored by matching until that person is registered again).
     */
    suspend fun reembedAll(embed: suspend (Bitmap) -> FloatArray): Int {
        val snapshot = synchronized(lock) { readAll() }

        val updated = HashMap<String, FloatArray>()
        for (face in snapshot) {
            val crop = loadCrop(face) ?: continue
            updated[face.faceId] = embed(crop)
        }

        return synchronized(lock) {
            val rewritten = readAll().map { face ->
                val embedding = updated[face.faceId]
                if (embedding == null) {
                    face
                } else {
                    StoredFace(
                        faceId = face.faceId,
                        personId = face.personId,
                        embedding = embedding,
                        modelId = FaceConfig.MODEL_ID,
                        pipelineVersion = FaceConfig.PIPELINE_VERSION,
                        cropFileName = face.cropFileName,
                        createdAtMillis = face.createdAtMillis
                    )
                }
            }
            writeAll(rewritten)
            updated.size
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------------------------------------

    private fun isCurrent(face: StoredFace): Boolean =
        face.modelId == FaceConfig.MODEL_ID && face.pipelineVersion == FaceConfig.PIPELINE_VERSION

    private fun readAll(): MutableList<StoredFace> {
        if (!indexFile.baseFile.exists()) return mutableListOf()

        return try {
            val root = JSONObject(String(indexFile.readFully(), Charsets.UTF_8))
            val array = root.getJSONArray("faces")
            val faces = ArrayList<StoredFace>(array.length())
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                faces.add(
                    StoredFace(
                        faceId = item.getString("faceId"),
                        personId = item.getString("personId"),
                        embedding = decode(item.getString("embedding")),
                        modelId = item.getString("modelId"),
                        pipelineVersion = item.getString("pipelineVersion"),
                        cropFileName = item.getString("crop"),
                        createdAtMillis = item.getLong("createdAt")
                    )
                )
            }
            faces
        } catch (e: Exception) {
            // Never crash the app on a damaged index: keep the file for inspection and start empty.
            Log.e("FaceTemplateStore", "Face index unreadable, moving it aside", e)
            indexFile.baseFile.renameTo(File(rootDir, "index.corrupt.${System.currentTimeMillis()}.json"))
            mutableListOf()
        }
    }

    private fun writeAll(faces: List<StoredFace>) {
        val array = JSONArray()
        for (face in faces) {
            array.put(
                JSONObject()
                    .put("faceId", face.faceId)
                    .put("personId", face.personId)
                    .put("embedding", encode(face.embedding))
                    .put("modelId", face.modelId)
                    .put("pipelineVersion", face.pipelineVersion)
                    .put("crop", face.cropFileName)
                    .put("createdAt", face.createdAtMillis)
            )
        }
        val root = JSONObject().put("version", 1).put("faces", array)

        val out = indexFile.startWrite()
        try {
            out.write(root.toString().toByteArray(Charsets.UTF_8))
            indexFile.finishWrite(out)
        } catch (e: Exception) {
            indexFile.failWrite(out)
            throw e
        }
    }

    private fun encode(values: FloatArray): String {
        val buffer = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (v in values) buffer.putFloat(v)
        return Base64.encodeToString(buffer.array(), Base64.NO_WRAP)
    }

    private fun decode(text: String): FloatArray {
        val bytes = Base64.decode(text, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / 4) { buffer.getFloat() }
    }
}
