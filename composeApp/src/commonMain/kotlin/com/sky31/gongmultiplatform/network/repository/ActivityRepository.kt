package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.CampusActivity
import com.sky31.gongmultiplatform.model.ActivityOrganizers
import com.sky31.gongmultiplatform.model.PublicFeatures
import com.sky31.gongmultiplatform.util.NetworkResult

interface ActivityRepository {
    suspend fun getActivities(limit: Int? = null): NetworkResult<List<CampusActivity>>
    suspend fun getActivityFilters(): NetworkResult<List<String>>
    suspend fun getActivityOrganizers(): NetworkResult<ActivityOrganizers>
    suspend fun getPublicFeatures(): NetworkResult<PublicFeatures>
}
