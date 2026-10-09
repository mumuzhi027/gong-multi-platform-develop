package com.sky31.gongmultiplatform.network.service

import kotlinx.coroutines.flow.Flow

enum class UpdateDownloadPhase { IDLE, DOWNLOADING, PAUSED, READY, INSTALLING, FAILED }

data class UpdateDownloadState(
    val phase: UpdateDownloadPhase = UpdateDownloadPhase.IDLE,
    val progress: Int = 0,
    val message: String = ""
)

/** The observer belongs to the UI; the download itself must survive its cancellation. */
expect object InstallService {
    fun observeDownload(url: String, version: String): Flow<UpdateDownloadState>

    suspend fun startDownload(url: String, version: String, autoInstall: Boolean)

    suspend fun installApk(url: String, version: String)
}
