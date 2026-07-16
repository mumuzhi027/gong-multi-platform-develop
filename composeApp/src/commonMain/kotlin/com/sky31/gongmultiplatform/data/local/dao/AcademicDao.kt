package com.sky31.gongmultiplatform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sky31.gongmultiplatform.data.local.domain.AcademicEntity

@Dao
interface AcademicDao {
    @Query("SELECT * from academicEntity")
    suspend fun getAcademicEntity(): AcademicEntity?

    @Update
    suspend fun updateAcademicEntity(entity: AcademicEntity)

    @Query("SELECT major_score from academicEntity")
    suspend fun getMajorScore(): String?

    @Query("SELECT minor_score from academicEntity")
    suspend fun getMinorScore(): String?

    @Query("SELECT compulsory_rank from academicEntity")
    suspend fun getCompulsoryRank(): String?

    @Query("SELECT total_rank from academicEntity")
    suspend fun getTotalRank(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicEntity(data: AcademicEntity)

    @Query("UPDATE academicEntity SET total_rank = :rank")
    suspend fun updateTotalRank(rank: String): Int

    @Query("UPDATE academicEntity SET compulsory_rank = :rank")
    suspend fun updateCompulsoryRank(rank: String): Int

    @Query("UPDATE academicEntity SET major_score = :score")
    suspend fun updateMajorScore(score: String): Int

    @Query("UPDATE academicEntity SET minor_score = :score")
    suspend fun updateMinorScore(score: String): Int

    @Query("DELETE FROM academicEntity")
    suspend fun clearAll()
}