package com.mardous.projectmusic.core.model.equalizer

data class EqSession(
    val type: SessionType,
    val id: Int,
    val active: Boolean
) {
    enum class SessionType {
        Internal, External
    }
}