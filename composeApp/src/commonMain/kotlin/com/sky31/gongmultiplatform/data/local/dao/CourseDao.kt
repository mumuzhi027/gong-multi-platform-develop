package com.sky31.gongmultiplatform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sky31.gongmultiplatform.data.local.domain.CourseEntity

@Dao
interface CourseDao {
    @Query("SELECT * FROM courseEntity")
    suspend fun getCourseEntity(): CourseEntity?

    @Update
    suspend fun updateCourseEntity(entity: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseEntity(data: CourseEntity)

    @Query("DELETE FROM courseEntity")
    suspend fun clearAll()
}