package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.domain.UserInfoEntity

interface UserInfoEntitySource {

    suspend fun getUserInfoEntity(): UserInfoEntity?

    suspend fun insertUserInfoEntity(entity: UserInfoEntity)

    suspend fun deleteAllUserInfoEntities()
}