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

    override suspend fun getMajorAcademicInfo(forceRefresh: Boolean): HttpResponse =
        client.get(MajorAcademicInfo(refresh = forceRefresh))


    override suspend fun getMinorAcademicInfo(forceRefresh: Boolean): HttpResponse =
        client.get(MinorAcademicInfo(refresh = forceRefresh))

    override suspend fun getTotalRank(forceRefresh: Boolean): HttpResponse =
        client.get(TotalRank(refresh = forceRefresh))

    override suspend fun getCompulsoryRank(forceRefresh: Boolean): HttpResponse =
        client.get(CompulsoryRank(refresh = forceRefresh))
}
