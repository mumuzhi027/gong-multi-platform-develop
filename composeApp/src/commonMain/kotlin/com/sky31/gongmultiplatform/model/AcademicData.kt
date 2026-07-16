package com.sky31.gongmultiplatform.model

data class AcademicData(
    var totalRank: RankData? = null,
    val compulsoryRank: RankData? = null,
    val majorScore: ScoreData? = null,
    val minorScore: ScoreData? = null,
)
