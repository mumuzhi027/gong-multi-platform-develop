package com.sky31.gongmultiplatform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sky31.gongmultiplatform.data.local.domain.ExamEntity

@Dao
interface ExamDao {
    @Query("SELECT * FROM examEntity")
    suspend fun getExamEntity(): ExamEntity?

    @Update
    suspend fun updateExamEntity(entity: ExamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamEntity(examEntity: ExamEntity)

    @Query("DELETE FROM examEntity")
    suspend fun clearAll()
}