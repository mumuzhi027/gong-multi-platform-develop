package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.data.local.dao.AcademicDao
import com.sky31.gongmultiplatform.data.local.domain.AcademicEntity
import com.sky31.gongmultiplatform.data.local.source.AcademicEntitySourceImpl
import com.sky31.gongmultiplatform.model.AcademicData
import com.sky31.gongmultiplatform.model.RankData
import com.sky31.gongmultiplatform.model.ScoreData
import com.sky31.gongmultiplatform.util.decodeOrNull
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AcademicDataRepositoryImpl(
    dao: AcademicDao
): AcademicDataRepository {

    private val source = AcademicEntitySourceImpl(dao)
    private val writeMutex = Mutex()

    override suspend fun updateAcademicData(
        totalRank: RankData?,
        compulsoryRank: RankData?,
        majorScore: ScoreData?,
        minorScore: ScoreData?
    ) {
        writeMutex.withLock {
            val oldEntity = source.getAcademicEntity()

            val entity = AcademicEntity(
                totalRank = totalRank?.let { Json.encodeToString(it) } ?: oldEntity?.totalRank,
                compulsoryRank = compulsoryRank?.let { Json.encodeToString(it) } ?: oldEntity?.compulsoryRank,
                majorScore = majorScore?.let { Json.encodeToString(it) } ?: oldEntity?.majorScore,
                minorScore = minorScore?.let { Json.encodeToString(it) } ?: oldEntity?.minorScore,
            )

            source.insertAcademicEntity(entity)
        }
    }

    override suspend fun getMajorScore(): ScoreData? {
        return decodeOrNull(
            raw = source.getMajorScore(),
            label = "major score"
        )
    }

    override suspend fun getMinorScore(): ScoreData? {
        return decodeOrNull(
            raw = source.getMinorScore(),
            label = "minor score"
        )
    }

    override suspend fun getTotalRank(): RankData? {
        return decodeOrNull(
            raw = source.getTotalRank(),
            label = "total rank"
        )
    }

    override suspend fun getCompulsoryRank(): RankData? {
        return decodeOrNull(
            raw = source.getCompulsoryRank(),
            label = "compulsory rank"
        )
    }

    override suspend fun getAcademicData(): AcademicData? {
        val entity = source.getAcademicEntity()

        return entity?.let {
            AcademicData(
                totalRank = decodeOrNull(it.totalRank, "academic total rank"),
                compulsoryRank = decodeOrNull(it.compulsoryRank, "academic compulsory rank"),
                majorScore = decodeOrNull(it.majorScore, "academic major score"),
                minorScore = decodeOrNull(it.minorScore, "academic minor score")
            )
        }
    }

    override suspend fun deleteAllAcademicData() {
        source.deleteAllAcademicEntities()
    }
}
