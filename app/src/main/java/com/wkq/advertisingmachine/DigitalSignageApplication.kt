package com.wkq.advertisingmachine

import android.app.Application
import com.wkq.advertisingmachine.control.SignageManager
import com.wkq.advertisingmachine.http.SignageHttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DigitalSignageApplication : Application() {

    lateinit var signageManager: SignageManager
        private set

    lateinit var httpServer: SignageHttpServer
        private set

    companion object {
        lateinit var instance: DigitalSignageApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. 初始化 Manager
        signageManager = SignageManager(this)

        // 2. 初始化 HTTP Server
        httpServer = SignageHttpServer(signageManager)

        // 3. 自动读取配置并启动 Server
        CoroutineScope(Dispatchers.IO).launch {
            val config = signageManager.configStore.configFlow.first()
            try {
                httpServer.start(config.port)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onTerminate() {
        httpServer.stop()
        signageManager.release()
        super.onTerminate()
    }
}
