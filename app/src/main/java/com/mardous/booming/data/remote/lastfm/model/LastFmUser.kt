package com.mardous.booming.data.remote.lastfm.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LastFmUserResponse(
    val user: LastFmUser
)

@Serializable
data class LastFmUser(
    val name: String,
    @SerialName("realname")
    val realName: String,
    val url: String,
    val image: List<LastFmImage>? = null,
    val playcount: String? = null,
    val registered: LastFmUserRegistered? = null
)

@Serializable
data class LastFmUserRegistered(
    val unixtime: String,
    @SerialName("#text")
    val text: String
)
