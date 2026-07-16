package com.sky31.gongmultiplatform.di

import com.sky31.gongmultiplatform.data.local.dao.AcademicDao
import com.sky31.gongmultiplatform.data.local.dao.ConfigDao
import com.sky31.gongmultiplatform.data.local.dao.CourseDao
import com.sky31.gongmultiplatform.data.local.dao.ExamDao
import com.sky31.gongmultiplatform.data.local.dao.PublicDao
import com.sky31.gongmultiplatform.data.local.dao.UserInfoDao
import com.sky31.gongmultiplatform.data.repository.AcademicDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.ConfigRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.CourseDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.ExamDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.PublicDataRepositoryImpl
import com.sky31.gongmultiplatform.data.repository.UserInfoDataRepositoryImpl
import org.koin.core.module.Module
import org.koin.dsl.module

val repositoryModule: Module = module {

    single<AcademicDataRepositoryImpl> { AcademicDataRepositoryImpl(dao = get<AcademicDao>()) }
    single<CourseDataRepositoryImpl> { CourseDataRepositoryImpl(dao = get<CourseDao>()) }
    single<ExamDataRepositoryImpl> { ExamDataRepositoryImpl(dao = get<ExamDao>()) }
    single<PublicDataRepositoryImpl> { PublicDataRepositoryImpl(dao = get<PublicDao>()) }
    single<ConfigRepositoryImpl> { ConfigRepositoryImpl(dao = get<ConfigDao>()) }
    single<UserInfoDataRepositoryImpl> { UserInfoDataRepositoryImpl(dao = get<UserInfoDao>()) }
}