package com.sky31.gongmultiplatform.data.local.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configEntity")
data class ConfigEntity(
    @PrimaryKey val uid: Int? = 1,
    @ColumnInfo(name = "global_config") val globalConfig: String? = null,
    @ColumnInfo(name = "functional_config") val functionalConfig: String? = null
)
