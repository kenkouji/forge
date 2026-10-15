package com.remoteforge.app

import android.app.Application
import com.remoteforge.app.data.config.NetworkConfig
import com.remoteforge.app.data.repository.RemoteForgeRepository

class RemoteForgeApplication : Application() {
    lateinit var networkConfig: NetworkConfig
        private set
    lateinit var repository: RemoteForgeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        networkConfig = NetworkConfig(this)
        repository = RemoteForgeRepository(networkConfig, this)
    }
}

