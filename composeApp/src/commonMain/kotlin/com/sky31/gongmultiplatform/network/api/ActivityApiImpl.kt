package com.sky31.gongmultiplatform.network.api

import com.sky31.gongmultiplatform.network.resources.Activities
import com.sky31.gongmultiplatform.network.resources.ActivityFilters
import com.sky31.gongmultiplatform.network.resources.ActivityOrganizersResource
import com.sky31.gongmultiplatform.network.resources.PublicFeaturesResource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.get
import io.ktor.client.statement.HttpResponse

class ActivityApiImpl(
    private val client: HttpClient
) : ActivityApi {
    override suspend fun getActivities(limit: Int?): HttpResponse =
        client.get(Activities(limit = limit))

    override suspend fun getActivityFilters(): HttpResponse = client.get(ActivityFilters())
    override suspend fun getActivityOrganizers(): HttpResponse = client.get(ActivityOrganizersResource())
    override suspend fun getPublicFeatures(): HttpResponse = client.get(PublicFeaturesResource())
}
