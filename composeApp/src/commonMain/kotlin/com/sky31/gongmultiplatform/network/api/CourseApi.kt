package com.sky31.gongmultiplatform.network.api

import io.ktor.client.statement.HttpResponse

interface CourseApi {

    suspend fun getCourses(): HttpResponse
}