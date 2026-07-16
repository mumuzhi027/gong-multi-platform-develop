package com.sky31.gongmultiplatform.di

import com.russhwolf.settings.Settings
import com.sky31.gongmultiplatform.util.createEncryptedSettings
import org.koin.core.module.Module
import org.koin.dsl.module

val securityModule: Module = module {
    single {
        createEncryptedSettings(
            get<Settings.Factory>(),
            name = "token"
        )
    }
}