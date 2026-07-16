package com.sky31.gongmultiplatform.ui.viewModel

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.Settings
import com.sky31.gongmultiplatform.network.dto.UpdateDto
import com.sky31.gongmultiplatform.network.repository.NotificationRepositoryImpl
import com.sky31.gongmultiplatform.util.AppUpdateState
import com.sky31.gongmultiplatform.util.DataState
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.PlatformInfo
import com.sky31.gongmultiplatform.util.createEncryptedSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SettingViewModel: ViewModel(), KoinComponent {
    private val notificationRepository: NotificationRepositoryImpl by inject()
    private val settingsFactory: Settings.Factory by inject()
    val platformInfo: PlatformInfo by inject()
    private val updateSettings: Settings by lazy {
        createEncryptedSettings(settingsFactory, name = "app_update")
    }

    private val _updateData = MutableStateFlow<UpdateDto?>(null)
    val updateData = _updateData.asStateFlow()

    private val _topBarTitle = MutableStateFlow("")
    val topBarTitle = _topBarTitle.asStateFlow()

    fun setTopBarTitle(title: String) {
        _topBarTitle.value = title
    }

    fun shouldShowAutoUpdateDialog(
        data: UpdateDto,
        appUpdateState: AppUpdateState
    ): Boolean {
        if (appUpdateState != AppUpdateState.OPTIONAL_UPDATE) {
            return appUpdateState == AppUpdateState.REQUIRED_UPDATE
        }

        val skippedVersion = runCatching {
            updateSettings.getStringOrNull(KEY_SKIPPED_VERSION)
        }.getOrElse {
            println("Failed to read skipped update version: ${it.message}")
            null
        }
        val skipUntil = runCatching {
            updateSettings.getLongOrNull(KEY_SKIP_UNTIL) ?: 0L
        }.getOrElse {
            println("Failed to read skipped update deadline: ${it.message}")
            0L
        }
        val now = Clock.System.now().toEpochMilliseconds()

        if (skippedVersion != data.lastVersion) {
            return true
        }

        return now >= skipUntil
    }

    fun suppressUpdateForWeek(data: UpdateDto) {
        val nextPromptTime = Clock.System.now().toEpochMilliseconds() + ONE_WEEK_MILLIS
        runCatching {
            updateSettings.putString(KEY_SKIPPED_VERSION, data.lastVersion)
            updateSettings.putLong(KEY_SKIP_UNTIL, nextPromptTime)
        }.onFailure {
            println("Failed to persist skipped update state: ${it.message}")
        }
    }

    fun clearSkippedUpdate(data: UpdateDto? = null) {
        val skippedVersion = runCatching {
            updateSettings.getStringOrNull(KEY_SKIPPED_VERSION)
        }.getOrElse {
            println("Failed to read skipped update version for clear: ${it.message}")
            null
        }
        if (data == null || skippedVersion == data.lastVersion) {
            runCatching {
                updateSettings.remove(KEY_SKIPPED_VERSION)
                updateSettings.remove(KEY_SKIP_UNTIL)
            }.onFailure {
                println("Failed to clear skipped update state: ${it.message}")
            }
        }
    }

    suspend fun checkUpdate(): DataState {
        val result = notificationRepository.getUpdateNotification()

        when(result) {
            is NetworkResult.Success -> {
                _updateData.value = result.data
                return DataState.Newest
            }
            is NetworkResult.Error -> {
                return DataState.Error(result.message)
            }
        }
    }

    private companion object {
        const val KEY_SKIPPED_VERSION = "skipped_update_version"
        const val KEY_SKIP_UNTIL = "skipped_update_until"
        const val ONE_WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L
    }
}
