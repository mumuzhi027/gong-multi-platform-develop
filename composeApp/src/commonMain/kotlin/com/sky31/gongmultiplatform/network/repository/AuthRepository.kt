package com.sky31.gongmultiplatform.network.repository

import com.sky31.gongmultiplatform.network.dto.AuthDto
import com.sky31.gongmultiplatform.util.NetworkResult

interface AuthRepository {

    suspend fun login(username: String, password: String): NetworkResult<AuthDto>
}