package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.data.local.dao.UserInfoDao
import com.sky31.gongmultiplatform.data.local.domain.UserInfoEntity
import com.sky31.gongmultiplatform.data.local.source.UserInfoEntitySourceImpl
import com.sky31.gongmultiplatform.model.UserInfo

class UserInfoDataRepositoryImpl(
    dao: UserInfoDao
): UserInfoDataRepository {
    private val source = UserInfoEntitySourceImpl(dao)

    override suspend fun getUserInfo(): UserInfo? {
        val userInfo = source.getUserInfoEntity()

        return userInfo?.let {
            UserInfo(
                studentID = it.studentID,
                name = it.name ?: "",
                gender = it.gender ?: "",
                birthday = it.birthday ?: "",
                major = it.major ?: "",
                clazz = it.clazz ?: "",
                entranceDay = it.entranceDay ?: ""
            )
        }
    }

    override suspend fun insertUserInfo(
        studentID: String,
        name: String?,
        gender: String?,
        birthday: String?,
        major: String?,
        clazz: String?,
        entranceDay: String?
    ) {
        val entity = UserInfoEntity(studentID, name, gender, birthday, major, clazz, entranceDay)

        source.insertUserInfoEntity(entity)
    }

    override suspend fun deleteAllUserInfo() {
        source.deleteAllUserInfoEntities()
    }
}