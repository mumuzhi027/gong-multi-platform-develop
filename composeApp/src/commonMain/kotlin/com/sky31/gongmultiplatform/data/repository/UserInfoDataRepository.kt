package com.sky31.gongmultiplatform.data.repository

import com.sky31.gongmultiplatform.model.UserInfo

interface UserInfoDataRepository {

    suspend fun insertUserInfo(
        studentID: String,
        name: String? = null,
        gender: String? = null,
        birthday: String? = null,
        major: String? = null,
        clazz: String? = null,
        entranceDay: String? = null
    )

    suspend fun getUserInfo(): UserInfo?

    suspend fun deleteAllUserInfo()
}