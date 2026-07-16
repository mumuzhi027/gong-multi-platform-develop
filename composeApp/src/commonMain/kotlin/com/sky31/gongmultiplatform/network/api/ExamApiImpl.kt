package com.sky31.gongmultiplatform.network.api

import com.sky31.gongmultiplatform.network.resources.Exam
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.get
import io.ktor.client.statement.HttpResponse

class ExamApiImpl(
    private val client: HttpClient
): ExamApi {

    override suspend fun getExamList(): HttpResponse {
        return client.get(Exam())
    }
}