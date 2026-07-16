package com.sky31.gongmultiplatform.util

import com.russhwolf.settings.Settings

fun createEncryptedSettings(factory: Settings.Factory, name: String): Settings {
    return factory.create(name)
}