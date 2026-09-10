package com.sky31.gongmultiplatform.ui.screen.launchScreen

import com.russhwolf.settings.Settings
import com.sky31.gongmultiplatform.util.createEncryptedSettings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** 记录某个节日在某一天已经展示启动页的次数。 */
object HolidayLaunchHistory : KoinComponent {
    private const val KEY_SHOW_COUNT_PREFIX = "show_count_v2_"

    private val settingsFactory: Settings.Factory by inject()
    private val settings: Settings by lazy {
        createEncryptedSettings(settingsFactory, name = "holiday_launch")
    }

    fun shouldShow(occurrence: HolidayLaunchOccurrence): Boolean = runCatching {
        settings.getInt(showCountKey(occurrence), defaultValue = 0) <
            HOLIDAY_LAUNCH_MAX_SHOW_COUNT_PER_DAY
    }.getOrElse {
        println("Failed to read holiday launch history: ${it.message}")
        true
    }

    fun markShown(occurrence: HolidayLaunchOccurrence) {
        runCatching {
            val key = showCountKey(occurrence)
            val shownCount = settings.getInt(key, defaultValue = 0)
            settings.putInt(key, shownCount + 1)
        }.onFailure {
            println("Failed to save holiday launch history: ${it.message}")
        }
    }

    private fun showCountKey(occurrence: HolidayLaunchOccurrence): String =
        KEY_SHOW_COUNT_PREFIX + occurrence.storageKey
}
