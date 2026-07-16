package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.domain.ConfigEntity

interface ConfigEntitySource {

    suspend fun insertConfigEntity(entity: ConfigEntity)

    suspend fun updateConfigEntity(entity: ConfigEntity)

    suspend fun getConfigEntity(): ConfigEntity?

    suspend fun getGlobalConfig(): String?

    suspend fun getFunctionalConfig(): String?

    suspend fun updateFunctionalConfig(config: String)

    suspend fun deleteAllConfigs()
}