package com.yunmei.client

import android.app.Application
import com.clj.fastble.BleManager
import com.yunmei.client.core.di.AppContainer

class YunMeiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        BleManager.getInstance().init(this)
        container = AppContainer(this)
    }
}
