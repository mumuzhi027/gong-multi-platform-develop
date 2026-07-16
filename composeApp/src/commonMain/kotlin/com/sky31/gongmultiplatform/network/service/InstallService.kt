package com.sky31.gongmultiplatform.network.service

import kotlinx.coroutines.flow.Flow


expect object InstallService {
    suspend fun downloadApk(url: String): Flow<Int>

    suspend fun installApk()
}