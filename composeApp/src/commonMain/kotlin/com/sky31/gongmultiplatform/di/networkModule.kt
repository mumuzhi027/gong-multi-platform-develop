package com.sky31.gongmultiplatform.di

import com.sky31.gongmultiplatform.network.HttpClientProvider
import com.sky31.gongmultiplatform.network.repository.AcademicRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.AuthRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.CourseRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.ExamRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.NotificationRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.PublicRepositoryImpl
import com.sky31.gongmultiplatform.network.repository.UserInfoRepositoryImpl
import org.koin.dsl.module

val networkModule = module {
    single { HttpClientProvider }
    single<AcademicRepositoryImpl> { AcademicRepositoryImpl(get<HttpClientProvider>().client) }
    single<ExamRepositoryImpl> { ExamRepositoryImpl(get<HttpClientProvider>().client) }
    single<CourseRepositoryImpl> { CourseRepositoryImpl(get<HttpClientProvider>().client) }
    single<PublicRepositoryImpl> { PublicRepositoryImpl(get<HttpClientProvider>().client) }
    single<AuthRepositoryImpl> { AuthRepositoryImpl(get<HttpClientProvider>().client) }
    single<NotificationRepositoryImpl> { NotificationRepositoryImpl() }
    single<UserInfoRepositoryImpl> { UserInfoRepositoryImpl(get<HttpClientProvider>().client) }
}