package com.sky31.gongmultiplatform.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sky31.gongmultiplatform.data.local.dao.AcademicDao
import com.sky31.gongmultiplatform.data.local.dao.ConfigDao
import com.sky31.gongmultiplatform.data.local.dao.CourseDao
import com.sky31.gongmultiplatform.data.local.dao.ExamDao
import com.sky31.gongmultiplatform.data.local.dao.PublicDao
import com.sky31.gongmultiplatform.data.local.dao.UserInfoDao
import com.sky31.gongmultiplatform.data.local.domain.AcademicEntity
import com.sky31.gongmultiplatform.data.local.domain.ConfigEntity
import com.sky31.gongmultiplatform.data.local.domain.CourseEntity
import com.sky31.gongmultiplatform.data.local.domain.ExamEntity
import com.sky31.gongmultiplatform.data.local.domain.PublicEntity
import com.sky31.gongmultiplatform.data.local.domain.UserInfoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [AcademicEntity::class, CourseEntity::class, ConfigEntity::class, ExamEntity::class, PublicEntity::class, UserInfoEntity::class],
    version = 4
)
abstract class AppDatabase: RoomDatabase()  {
    abstract fun getAcademicDao(): AcademicDao
    abstract fun getCourseDao(): CourseDao
    abstract fun getExamDao(): ExamDao
    abstract fun getPublicDao(): PublicDao
    abstract fun getConfigDao(): ConfigDao

    abstract fun getUserInfoDao(): UserInfoDao
}

fun getAppDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .addMigrations()
        .fallbackToDestructiveMigration(true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}