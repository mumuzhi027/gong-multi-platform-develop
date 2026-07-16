package com.sky31.gongmultiplatform.util

import android.content.Context
import com.sky31.gongmultiplatform.di.authViewModelModule
import com.sky31.gongmultiplatform.di.networkModule
import com.sky31.gongmultiplatform.di.repositoryModule
import com.sky31.gongmultiplatform.di.securityModule
import com.sky31.gongmultiplatform.di.viewModelModule
import com.sky31.gongmultiplatform.module.androidDatabaseModule
import com.sky31.gongmultiplatform.module.androidSecurityModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

fun initKoin(context: Context) {
    if (GlobalContext.getOrNull() == null) { // 防止重复初始化
        startKoin {
            androidContext(context)
            modules(listOf(
                repositoryModule,
                androidDatabaseModule,
                androidSecurityModule,
                securityModule,
                networkModule,
                viewModelModule,
                authViewModelModule
            ))
        }
    }
}