package com.sky31.gongmultiplatform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sky31.gongmultiplatform.data.local.domain.PublicEntity

@Dao
interface PublicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPublicEntity(data: PublicEntity)

    @Update
    suspend fun updatePublicEntity(entity: PublicEntity)

    @Query("SELECT today_classroom FROM publicEntity")
    suspend fun getTodayClassroom(): String?

    @Query("SELECT tomorrow_classroom FROM publicEntity")
    suspend fun getTomorrowClassroom(): String?

    @Query("SELECT calendar FROM publicEntity")
    suspend fun getCalendar(): String?

    @Query("SELECT * FROM publicEntity")
    suspend fun getPublicEntity(): PublicEntity?

    @Query("DELETE FROM publicEntity")
    suspend fun clearAll()
}