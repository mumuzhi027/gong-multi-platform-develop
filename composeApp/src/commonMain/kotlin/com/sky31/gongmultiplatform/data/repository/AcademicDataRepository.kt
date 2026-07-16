package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.model.AcademicData
import com.sky31.gongmultiplatform.model.RankData
import com.sky31.gongmultiplatform.model.ScoreData

interface AcademicDataRepository {

    suspend fun updateAcademicData(
        totalRank: RankData? = null,
        compulsoryRank: RankData? = null,
        majorScore: ScoreData? = null,
        minorScore: ScoreData? = null,
    )

    suspend fun getMajorScore(): ScoreData?

    suspend fun getMinorScore(): ScoreData?

    suspend fun getTotalRank(): RankData?

    suspend fun getCompulsoryRank(): RankData?

    suspend fun getAcademicData(): AcademicData?

    suspend fun deleteAllAcademicData()
}