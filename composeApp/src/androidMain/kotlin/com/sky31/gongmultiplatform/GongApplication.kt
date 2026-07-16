package com.sky31.gongmultiplatform

import android.app.Application
import com.sky31.gongmultiplatform.util.initKoin

class GongApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        initKoin(this)
    }
}