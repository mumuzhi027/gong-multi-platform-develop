package com.sky31.gongmultiplatform.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sky31.gongmultiplatform.data.local.domain.ConfigEntity

@Dao
interface ConfigDao {
    @Query("SELECT * FROM configEntity")
    suspend fun getConfig(): ConfigEntity?

    @Query("SELECT global_config FROM configEntity")
    suspend fun getGlobalConfig(): String?

    @Query("SELECT functional_config FROM configEntity")
    suspend fun getFunctionalConfig(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfigEntity(data: ConfigEntity)

    @Update
    suspend fun updateConfigEntity(data: ConfigEntity)

    @Query("UPDATE configEntity SET functional_config = :config")
    suspend fun updateFunctionalConfig(config: String)

    @Query("DELETE FROM configEntity")
    suspend fun clearAll()
}