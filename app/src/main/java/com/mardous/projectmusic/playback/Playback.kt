package com.mardous.projectmusic.playback

object Playback {
    // Custom commands
    const val TOGGLE_SHUFFLE = "com.mardous.projectmusic.command.shuffle.toggle"
    const val CYCLE_REPEAT = "com.mardous.projectmusic.command.repeat.cycle"
    const val TOGGLE_FAVORITE = "com.mardous.projectmusic.command.toggle_favorite"
    const val SHOW_LYRICS = "com.mardous.projectmusic.command.show_lyrics"
    const val RESTORE_PLAYBACK = "com.mardous.projectmusic.command.restore_playback"

    const val SET_UNSHUFFLED_ORDER = "com.mardous.projectmusic.command.set.unshuffled_order"
    const val SET_STOP_POSITION = "com.mardous.projectmusic.command.set.stop_position"

    // MediaItem extras for queue context
    const val EXTRA_QUEUE_SOURCE = "com.mardous.projectmusic.extra.queue_source"
    const val EXTRA_PLAYLIST_ID = "com.mardous.projectmusic.extra.playlist_id"
    const val EXTRA_PLAYLIST_NAME = "com.mardous.projectmusic.extra.playlist_name"
    const val EXTRA_PLAYBACK_ORIGIN = "com.mardous.projectmusic.extra.playback_origin"

    // Custom events
    const val EVENT_MEDIA_CONTENT_CHANGED = "com.mardous.projectmusic.event.media_content_changed"
    const val EVENT_FAVORITE_CONTENT_CHANGED = "com.mardous.projectmusic.event.favorite_content_changed"
    const val EVENT_SHOW_LYRICS = "com.mardous.projectmusic.event.show_lyrics"
    const val EVENT_PLAYBACK_RESTORED = "com.mardous.projectmusic.event.playback_restored"
    const val EVENT_PLAYBACK_STARTED = "com.mardous.projectmusic.event.playback_started"
}