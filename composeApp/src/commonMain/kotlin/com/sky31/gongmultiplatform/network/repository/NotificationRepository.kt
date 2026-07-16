package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.network.dto.UpdateDto
import com.sky31.gongmultiplatform.util.NetworkResult

interface NotificationRepository {

    suspend fun getUpdateNotification(): NetworkResult<UpdateDto>
}