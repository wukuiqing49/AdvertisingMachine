package com.wkq.advertisingmachine

import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.wkq.advertisingmachine.databinding.ActivityMainBinding
import com.wkq.base.activity.BaseActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val app get() = application as DigitalSignageApplication

    override fun initView() {
        // 1. 注册主屏幕渲染视口
        app.signageManager.displayChannelManager.registerMainContainer(binding.logicalContainer)

        // 2. 动态监听配置变化，刷新 UI 显示
        lifecycleScope.launch {
            app.signageManager.configStore.configFlow.collectLatest { config ->
                val ip = getLocalIpAddress()
                binding.tvServerStatus.text = "Server Running (${config.deviceName})"
                binding.tvServerAddress.text = "$ip:${config.port}"
            }
        }

        // 3. 根视图、卡片视图、视口 Container 长按全部绑定跳转，保证任何地方都可以长按进入设置
        val longClickListener = android.view.View.OnLongClickListener {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
            true
        }

        binding.root.setOnLongClickListener(longClickListener)
        binding.logicalContainer.setOnLongClickListener(longClickListener)
        binding.serverInfoContainer.setOnLongClickListener(longClickListener)
    }

    override fun initData() = Unit

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val ip = address.hostAddress
                        if (ip != null && !ip.startsWith("127.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "0.0.0.0"
    }
}

