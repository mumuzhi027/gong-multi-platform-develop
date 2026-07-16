package com.sky31.gongmultiplatform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sky31.gongmultiplatform.data.local.domain.UserInfoEntity

@Dao
interface UserInfoDao {
    @Query("SELECT * FROM userInfoEntity")
    suspend fun getUserInfoEntity(): UserInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInfoEntity(data: UserInfoEntity)

    @Query("DELETE FROM userInfoEntity")
    suspend fun clearAll()
}