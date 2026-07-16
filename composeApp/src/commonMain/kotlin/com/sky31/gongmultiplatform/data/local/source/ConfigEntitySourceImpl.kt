package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.dao.ConfigDao
import com.sky31.gongmultiplatform.data.local.domain.ConfigEntity

class ConfigEntitySourceImpl(
    private val dao: ConfigDao
): ConfigEntitySource {

    override suspend fun insertConfigEntity(entity: ConfigEntity) {
        dao.insertConfigEntity(entity)
    }

    override suspend fun updateConfigEntity(entity: ConfigEntity) {
        dao.updateConfigEntity(entity)
    }

    override suspend fun getConfigEntity(): ConfigEntity? {
        return dao.getConfig()
    }

    override suspend fun getGlobalConfig(): String? {
        return dao.getGlobalConfig()
    }

    override suspend fun getFunctionalConfig(): String? {
        return dao.getFunctionalConfig()
    }

    override suspend fun updateFunctionalConfig(config: String) {
        dao.updateFunctionalConfig(config)
    }

    override suspend fun deleteAllConfigs() {
        dao.clearAll()
    }
}