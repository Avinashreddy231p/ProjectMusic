package com.mardous.projectmusic.ui.screen.settings.search

import android.content.Context
import androidx.annotation.XmlRes
import com.mardous.projectmusic.R
import org.xmlpull.v1.XmlPullParser

data class SettingsSearchResult(
    val key: String,
    val title: String,
    val summary: String,
    val destinationId: Int,
    val parentTitle: String
)

object SettingsSearchHelper {
    private val allResults = mutableListOf<SettingsSearchResult>()

    fun indexSettings(context: Context) {
        if (allResults.isNotEmpty()) return

        indexXml(context, R.xml.preferences_screen_appearance, R.id.nav_interface_preferences, "Appearance")
        indexXml(context, R.xml.preferences_screen_swipe_actions, R.id.nav_swipe_actions_preferences, "Swipe Actions")
        indexXml(context, R.xml.preferences_screen_now_playing, R.id.nav_now_playing_preferences, "Now Playing")
        indexXml(context, R.xml.preferences_screen_lyrics, R.id.nav_lyrics_preferences, "Lyrics")
        indexXml(context, R.xml.preferences_screen_playback, R.id.nav_playback_preferences, "Playback")
        indexXml(context, R.xml.preferences_screen_library, R.id.nav_library_preferences, "Library")
        indexXml(context, R.xml.preferences_screen_network, R.id.nav_network_preferences, "Network")
        indexXml(context, R.xml.preferences_screen_advanced, R.id.nav_advanced_preferences, "Advanced")
    }

    private fun indexXml(context: Context, @XmlRes xmlRes: Int, destinationId: Int, parentTitle: String) {
        val parser = context.resources.getXml(xmlRes)
        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val name = parser.name
                    if (name.endsWith("Preference") || name.endsWith("PreferenceCategory")) {
                        var titleStr = ""
                        var summaryStr = ""
                        var keyStr = ""
                        
                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i)
                            val attrResValue = parser.getAttributeResourceValue(i, 0)
                            val attrValue = parser.getAttributeValue(i)
                            
                            val resolvedStr = if (attrResValue != 0) {
                                try { context.getString(attrResValue) } catch (e: Exception) { attrValue }
                            } else {
                                attrValue
                            }
                            
                            when (attrName) {
                                "title" -> titleStr = resolvedStr
                                "summary" -> summaryStr = resolvedStr
                                "key" -> keyStr = resolvedStr 
                            }
                        }
                        if (titleStr.isNotEmpty() && keyStr.isNotEmpty()) {
                            allResults.add(SettingsSearchResult(keyStr, titleStr, summaryStr, destinationId, parentTitle))
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            parser.close()
        }
    }

    fun search(query: String): List<SettingsSearchResult> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase().trim()
        return allResults.filter {
            it.title.lowercase().contains(lowerQuery) || it.summary.lowercase().contains(lowerQuery)
        }
    }
}
