package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.data.local.dao.ConfigDao
import com.sky31.gongmultiplatform.data.local.domain.ConfigEntity
import com.sky31.gongmultiplatform.data.local.source.ConfigEntitySourceImpl
import com.sky31.gongmultiplatform.model.config.AuthConfig
import com.sky31.gongmultiplatform.model.config.FunctionalConfig
import com.sky31.gongmultiplatform.model.config.GlobalConfig
import com.sky31.gongmultiplatform.model.config.NotificationConfig
import com.sky31.gongmultiplatform.ui.theme.ThemeMode
import com.sky31.gongmultiplatform.util.decodeOrNull
import kotlinx.serialization.json.Json

class ConfigRepositoryImpl(
    dao: ConfigDao
): ConfigRepository {

    private val source = ConfigEntitySourceImpl(dao)

    override suspend fun insertConfig(globalConfig: GlobalConfig, functionalConfig: FunctionalConfig) {
        source.insertConfigEntity(ConfigEntity(
            globalConfig = Json.encodeToString<GlobalConfig>(globalConfig),
            functionalConfig = Json.encodeToString<FunctionalConfig>(functionalConfig)
        ))
    }

    override suspend fun updateConfig(
        themeMode: ThemeMode?
    ) {
        val oldEntity = source.getConfigEntity()
        val oldGlobalConfig = decodeOrNull<GlobalConfig>(
            raw = oldEntity?.globalConfig,
            label = "global config"
        ) ?: GlobalConfig()

        val globalConfig = GlobalConfig(
            themeMode = themeMode ?: oldGlobalConfig.themeMode
        )

        val entity = ConfigEntity(
            globalConfig = Json.encodeToString<GlobalConfig>(globalConfig),
            functionalConfig = oldEntity?.functionalConfig
        )

        source.insertConfigEntity(entity)
    }

    override suspend fun updateFunctionalConfig(config: FunctionalConfig) {
        source.updateFunctionalConfig(Json.encodeToString(config))
    }

    override suspend fun getGlobalConfig(): GlobalConfig? {
        return decodeOrNull(
            raw = source.getGlobalConfig(),
            label = "global config"
        )
    }

    override suspend fun getFunctionalConfig(): FunctionalConfig? {
        return decodeOrNull(
            raw = source.getFunctionalConfig(),
            label = "functional config"
        )
    }

    override suspend fun getAuthConfig(): AuthConfig? {
        return getFunctionalConfig()?.authConfig
    }

    override suspend fun getNotificationConfig(): NotificationConfig? {
        return getFunctionalConfig()?.notificationConfig
    }

    override suspend fun deleteConfig() {
        source.deleteAllConfigs()
    }
}
