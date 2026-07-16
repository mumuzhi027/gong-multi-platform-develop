package com.sky31.gongmultiplatform.network.api

import com.sky31.gongmultiplatform.network.resources.Calendar
import com.sky31.gongmultiplatform.network.resources.Classroom
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.get
import io.ktor.client.statement.HttpResponse

class PublicApiImpl(
    private val client: HttpClient
): PublicApi {

    override suspend fun getTodayClassroom(): HttpResponse =
        client.get(Classroom.TodayClassroom())

    override suspend fun getTomorrowClassroom(): HttpResponse =
        client.get(Classroom.TomorrowClassroom())

    override suspend fun getCalendar(): HttpResponse =
        client.get(Calendar())
}