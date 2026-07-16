package com.sky31.gongmultiplatform.module

import com.sky31.gongmultiplatform.data.local.AppDatabase
import com.sky31.gongmultiplatform.data.local.dao.AcademicDao
import com.sky31.gongmultiplatform.data.local.dao.ConfigDao
import com.sky31.gongmultiplatform.data.local.dao.CourseDao
import com.sky31.gongmultiplatform.data.local.dao.ExamDao
import com.sky31.gongmultiplatform.data.local.dao.PublicDao
import com.sky31.gongmultiplatform.data.local.dao.UserInfoDao
import com.sky31.gongmultiplatform.data.local.getAppDatabase
import com.sky31.gongmultiplatform.db.getDatabaseBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidDatabaseModule = module {
    single { getAppDatabase(getDatabaseBuilder(androidContext())) }

    // 提供 DAO
    single<AcademicDao> { get<AppDatabase>().getAcademicDao() }
    single<CourseDao> { get<AppDatabase>().getCourseDao() }
    single<ExamDao> { get<AppDatabase>().getExamDao() }
    single<PublicDao> { get<AppDatabase>().getPublicDao() }
    single<ConfigDao> { get<AppDatabase>().getConfigDao() }
    single<UserInfoDao> { get<AppDatabase>().getUserInfoDao() }
}