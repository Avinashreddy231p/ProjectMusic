/*
 * Copyright (c) 2026 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mardous.projectmusic.data.remote.github

import android.content.Context
import com.mardous.projectmusic.BuildConfig
import com.mardous.projectmusic.data.remote.github.model.GitHubRelease
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import kotlin.time.ExperimentalTime

class GitHubService(private val context: Context, private val client: HttpClient, private val authToken: String? = null) {

    private suspend fun get(url: String): HttpResponse {
        return client.get(url) {
            authToken?.let {
                headers { append("Authorization", "token $it") }
            }
        }
    }

    private suspend fun fetchAllReleases(user: String, repo: String, page: Int = 1, limit: Int = 20): List<GitHubRelease> =
        get("${GITHUB_API_URL}repos/$user/$repo/releases?page=$page&per_page=$limit").body()

    @OptIn(ExperimentalTime::class)
    suspend fun latestRelease(user: String = DEFAULT_USER, repo: String = DEFAULT_REPO, allowExperimental: Boolean = true): GitHubRelease {
        if (GITHUB_API_URL.isBlank()) {
            throw IllegalStateException("GitHub API URL is not configured for this flavor")
        }

        val allReleases = runCatching { fetchAllReleases(user, repo) }.getOrNull() 
            ?: throw IllegalStateException("Failed to fetch releases from GitHub")

        val stableRelease = allReleases.firstOrNull { !it.isPrerelease && it.hasApk }
            ?: allReleases.firstOrNull { it.hasApk } // Fallback to any release with an APK if no stable ones exist

        if (allowExperimental) {
            val mostRecent = allReleases.filter { it.hasApk }.maxByOrNull { it.lastUpdatedAt }
            
            if (mostRecent != null && mostRecent.isNewer(context)) {
                return mostRecent
            }
        }

        return stableRelease ?: throw IllegalStateException("No suitable releases found on GitHub")
    }

    companion object {
        private const val GITHUB_API_URL = BuildConfig.GITHUB_API_URL

        private const val DEFAULT_USER = "Avinashreddy231p"
        private const val DEFAULT_REPO = "ProjectMusic"
    }
}