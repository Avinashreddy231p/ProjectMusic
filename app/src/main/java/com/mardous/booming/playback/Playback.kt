package com.mardous.booming.playback

object Playback {
    // Custom commands
    const val TOGGLE_SHUFFLE = "com.mardous.booming.command.shuffle.toggle"
    const val CYCLE_REPEAT = "com.mardous.booming.command.repeat.cycle"
    const val TOGGLE_FAVORITE = "com.mardous.booming.command.toggle_favorite"
    const val SHOW_LYRICS = "com.mardous.booming.command.show_lyrics"
    const val RESTORE_PLAYBACK = "com.mardous.booming.command.restore_playback"

    const val SET_UNSHUFFLED_ORDER = "com.mardous.booming.command.set.unshuffled_order"
    const val SET_STOP_POSITION = "com.mardous.booming.command.set.stop_position"

    // MediaItem extras for queue context
    const val EXTRA_QUEUE_SOURCE = "com.mardous.booming.extra.queue_source"
    const val EXTRA_PLAYLIST_ID = "com.mardous.booming.extra.playlist_id"
    const val EXTRA_PLAYLIST_NAME = "com.mardous.booming.extra.playlist_name"
    const val EXTRA_PLAYBACK_ORIGIN = "com.mardous.booming.extra.playback_origin"

    // Custom events
    const val EVENT_MEDIA_CONTENT_CHANGED = "com.mardous.booming.event.media_content_changed"
    const val EVENT_FAVORITE_CONTENT_CHANGED = "com.mardous.booming.event.favorite_content_changed"
    const val EVENT_SHOW_LYRICS = "com.mardous.booming.event.show_lyrics"
    const val EVENT_PLAYBACK_RESTORED = "com.mardous.booming.event.playback_restored"
    const val EVENT_PLAYBACK_STARTED = "com.mardous.booming.event.playback_started"
}