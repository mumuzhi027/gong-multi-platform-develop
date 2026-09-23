package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.CampusActivity
import com.sky31.gongmultiplatform.model.ActivityOrganizers
import com.sky31.gongmultiplatform.model.PublicFeatures
import com.sky31.gongmultiplatform.network.api.ActivityApiImpl
import com.sky31.gongmultiplatform.util.NetworkResult
import com.sky31.gongmultiplatform.util.safeApiCall
import io.ktor.client.HttpClient

class ActivityRepositoryImpl(
    client: HttpClient
) : ActivityRepository {
    private val api = ActivityApiImpl(client)

    override suspend fun getActivities(limit: Int?): NetworkResult<List<CampusActivity>> =
        safeApiCall { api.getActivities(limit) }

    override suspend fun getActivityFilters(): NetworkResult<List<String>> =
        safeApiCall { api.getActivityFilters() }

    override suspend fun getActivityOrganizers(): NetworkResult<ActivityOrganizers> =
        safeApiCall { api.getActivityOrganizers() }

    override suspend fun getPublicFeatures(): NetworkResult<PublicFeatures> =
        safeApiCall { api.getPublicFeatures() }
}
