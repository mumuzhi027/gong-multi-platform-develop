package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.model.UserInfo
import com.sky31.gongmultiplatform.util.NetworkResult

interface UserInfoRepository {
    suspend fun getUserInfo(): NetworkResult<UserInfo>
}