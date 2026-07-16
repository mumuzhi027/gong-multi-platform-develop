package com.sky31.gongmultiplatform.data.local.source

import com.sky31.gongmultiplatform.data.local.dao.UserInfoDao
import com.sky31.gongmultiplatform.data.local.domain.UserInfoEntity

class UserInfoEntitySourceImpl(
    private val dao: UserInfoDao
): UserInfoEntitySource {

    override suspend fun getUserInfoEntity(): UserInfoEntity? {
        return dao.getUserInfoEntity()
    }

    override suspend fun insertUserInfoEntity(entity: UserInfoEntity) {
        dao.insertUserInfoEntity(entity)
    }

    override suspend fun deleteAllUserInfoEntities() {
        dao.clearAll()
    }
}