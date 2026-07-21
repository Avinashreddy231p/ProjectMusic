package com.mardous.projectmusic.data.remote.lyrics

import android.util.Log
import com.mardous.projectmusic.data.model.Song
import com.mardous.projectmusic.data.remote.lyrics.model.UnisonJwk
import com.mardous.projectmusic.data.remote.lyrics.model.UnisonPayload
import com.mardous.projectmusic.data.remote.lyrics.model.UnisonSubmissionRequest
import com.mardous.projectmusic.util.CryptoUtil
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class UnisonSubmissionService(private val client: HttpClient) {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun submitLyrics(
        song: Song,
        lyrics: String,
        format: String
    ): Boolean {
        return try {
            val keyId = CryptoUtil.getUnisonPublicKeyId()
            val timestamp = System.currentTimeMillis()
            val nonce = UUID.randomUUID().toString().replace("-", "").take(16)
            
            val payload = UnisonPayload(
                keyId = keyId,
                timestamp = timestamp,
                nonce = nonce,
                song = song.title,
                artist = song.artistName,
                album = song.albumName,
                duration = (song.duration / 1000).toInt(),
                lyrics = lyrics,
                format = format,
                videoId = null // We don't have it easily available here
            )

            // Canonical JSON: alphabetical keys, no whitespace
            // We'll use a manual map to ensure order if necessary, 
            // but Kotlinx Serialization with sorted keys is better if configured.
            // For simplicity, we'll build it manually or use a TreeMap.
            val canonicalPayload = buildCanonicalJson(payload)
            val signature = CryptoUtil.signData(canonicalPayload.toByteArray(Charsets.UTF_8))
            
            val keyPair = CryptoUtil.getOrCreateUnisonKey()
            val publicKey = keyPair.public as java.security.interfaces.ECPublicKey
            val jwkString = CryptoUtil.buildUnisonJwk(publicKey)
            val jwk = Json.decodeFromString<UnisonJwk>(jwkString)

            val request = UnisonSubmissionRequest(
                payload = payload,
                signature = signature,
                publicKey = jwk
            )

            val response = client.post(SUBMIT_URL) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                Log.d(TAG, "Lyrics submitted successfully to Unison")
                true
            } else {
                Log.e(TAG, "Failed to submit lyrics: ${response.status}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting lyrics to Unison", e)
            false
        }
    }

    private fun buildCanonicalJson(payload: UnisonPayload): String {
        // Manual building to ensure strict canonical order:
        // album, artist, duration, format, keyId, lyrics, nonce, song, timestamp, videoId
        val sb = StringBuilder("{")
        if (payload.album != null) sb.append("\"album\":\"${payload.album}\",")
        sb.append("\"artist\":\"${payload.artist}\",")
        sb.append("\"duration\":${payload.duration},")
        sb.append("\"format\":\"${payload.format}\",")
        sb.append("\"keyId\":\"${payload.keyId}\",")
        sb.append("\"lyrics\":\"${escapeJson(payload.lyrics)}\",")
        sb.append("\"nonce\":\"${payload.nonce}\",")
        sb.append("\"song\":\"${payload.song}\",")
        sb.append("\"timestamp\":${payload.timestamp}")
        if (payload.videoId != null) sb.append(",\"videoId\":\"${payload.videoId}\"")
        sb.append("}")
        return sb.toString()
    }

    private fun escapeJson(text: String): String {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    companion object {
        private const val TAG = "UnisonSubmissionService"
        private const val SUBMIT_URL = "https://unison.boidu.dev/api/lyrics/submit"
    }
}
