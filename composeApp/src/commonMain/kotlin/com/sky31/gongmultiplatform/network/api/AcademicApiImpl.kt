package com.sky31.gongmultiplatform.network.api

import com.sky31.gongmultiplatform.network.resources.CompulsoryRank
import com.sky31.gongmultiplatform.network.resources.MajorAcademicInfo
import com.sky31.gongmultiplatform.network.resources.MinorAcademicInfo
import com.sky31.gongmultiplatform.network.resources.TotalRank
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.get
import io.ktor.client.statement.HttpResponse

class AcademicApiImpl(
    private val client: HttpClient
): AcademicApi {

    override suspend fun getMajorAcademicInfo(): HttpResponse =
        client.get(MajorAcademicInfo())


    override suspend fun getMinorAcademicInfo(): HttpResponse =
        client.get(MinorAcademicInfo())

    override suspend fun getTotalRank(): HttpResponse =
        client.get(TotalRank())

    override suspend fun getCompulsoryRank(): HttpResponse =
        client.get(CompulsoryRank())
}