package com.mardous.projectmusic.ui.screen.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateDownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            // No longer auto-installing.
            // The UpdateViewModel handles polling and manual installation.
        }
    }
}
