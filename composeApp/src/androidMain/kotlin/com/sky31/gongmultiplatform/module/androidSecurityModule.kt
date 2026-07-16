package com.sky31.gongmultiplatform.module

import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.sky31.gongmultiplatform.util.PlatformInfo
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val androidSecurityModule: Module = module {
    single<Settings.Factory> { SharedPreferencesSettings.Factory(androidContext()) }
    single<PlatformInfo> { PlatformInfo() }
}