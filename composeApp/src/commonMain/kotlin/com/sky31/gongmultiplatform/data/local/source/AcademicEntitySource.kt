package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.domain.AcademicEntity

interface AcademicEntitySource {
    suspend fun insertAcademicEntity(entity: AcademicEntity)

    suspend fun updateAcademicEntity(entity: AcademicEntity)

    suspend fun getMajorScore(): String?

    suspend fun getMinorScore(): String?

    suspend fun getTotalRank(): String?

    suspend fun getCompulsoryRank(): String?

    suspend fun getAcademicEntity(): AcademicEntity?

    suspend fun deleteAllAcademicEntities()
}