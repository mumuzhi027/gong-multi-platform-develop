package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.domain.PublicEntity

interface PublicEntitySource {

    suspend fun insertPublicEntity(entity: PublicEntity)

    suspend fun updatePublicEntity(entity: PublicEntity)

    suspend fun getPublicEntity(): PublicEntity?

    suspend fun getCalendar(): String?

    suspend fun getTodayClassroom(): String?

    suspend fun getTomorrowClassroom(): String?

    suspend fun deleteAllPublicEntities()
}