package com.mardous.booming.data.remote.listenbrainz.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListenBrainzSubmission(
    @SerialName("listen_type")
    val listenType: String,
    val payload: List<ListenBrainzListen>
)

@Serializable
data class ListenBrainzListen(
    @SerialName("listened_at")
    val listenedAt: Long? = null,
    @SerialName("track_metadata")
    val trackMetadata: ListenBrainzTrackMetadata
)

@Serializable
data class ListenBrainzTrackMetadata(
    @SerialName("artist_name")
    val artistName: String,
    @SerialName("track_name")
    val trackName: String,
    @SerialName("release_name")
    val releaseName: String? = null,
    @SerialName("additional_info")
    val additionalInfo: ListenBrainzTrackAdditionalInfo? = null
)

@Serializable
data class ListenBrainzTrackAdditionalInfo(
    @SerialName("media_player")
    val player: String? = null,
    @SerialName("media_player_version")
    val playerVersion: String? = null
)

@Serializable
data class ListenBrainzResponse(
    val status: String,
    val error: String? = null
)

@Serializable
data class ListenBrainzTokenValidationResponse(
    val code: Int,
    val message: String,
    val valid: Boolean,
    @SerialName("user_name")
    val userName: String? = null
)

@Serializable
data class ListenBrainzListensResponse(
    val payload: ListenBrainzListensPayload
)

@Serializable
data class ListenBrainzListensPayload(
    val count: Int,
    @SerialName("latest_listen_at")
    val latestListenAt: Long,
    val listens: List<ListenBrainzListen>,
    @SerialName("user_id")
    val userId: String
)

@Serializable
data class ListenBrainzUserListenCountResponse(
    val payload: ListenBrainzUserListenCountPayload
)

@Serializable
data class ListenBrainzUserListenCountPayload(
    val count: Int
)
