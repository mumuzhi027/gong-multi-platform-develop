package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.model.config.AuthConfig
import com.sky31.gongmultiplatform.model.config.FunctionalConfig
import com.sky31.gongmultiplatform.model.config.GlobalConfig
import com.sky31.gongmultiplatform.model.config.NotificationConfig
import com.sky31.gongmultiplatform.ui.theme.ThemeMode

interface ConfigRepository {

    suspend fun insertConfig(
        globalConfig: GlobalConfig = GlobalConfig(),
        functionalConfig: FunctionalConfig = FunctionalConfig()
    )

    suspend fun updateConfig(
        themeMode: ThemeMode? = null
    )

    suspend fun updateFunctionalConfig(
        config: FunctionalConfig
    )

    suspend fun getGlobalConfig(): GlobalConfig?

    suspend fun getFunctionalConfig(): FunctionalConfig?

    suspend fun getAuthConfig(): AuthConfig?

    suspend fun getNotificationConfig(): NotificationConfig?

    suspend fun deleteConfig()
}