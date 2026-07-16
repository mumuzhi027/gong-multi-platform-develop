package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.dao.PublicDao
import com.sky31.gongmultiplatform.data.local.domain.PublicEntity

class PublicEntitySourceImpl(
    private val dao: PublicDao
): PublicEntitySource {

    override suspend fun insertPublicEntity(entity: PublicEntity) {
        dao.insertPublicEntity(entity)
    }

    override suspend fun updatePublicEntity(entity: PublicEntity) {
        dao.updatePublicEntity(entity)
    }

    override suspend fun getPublicEntity(): PublicEntity? {
        return dao.getPublicEntity()
    }

    override suspend fun getCalendar(): String? {
        return dao.getCalendar()
    }

    override suspend fun getTodayClassroom(): String? {
        return dao.getTodayClassroom()
    }

    override suspend fun getTomorrowClassroom(): String? {
        return dao.getTomorrowClassroom()
    }

    override suspend fun deleteAllPublicEntities() {
        dao.clearAll()
    }
}