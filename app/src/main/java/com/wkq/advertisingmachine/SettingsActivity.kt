package com.wkq.advertisingmachine

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.wkq.advertisingmachine.databinding.ActivitySettingsBinding
import com.wkq.advertisingmachine.model.SignageConfig
import com.wkq.base.activity.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : BaseActivity<ActivitySettingsBinding>() {

    private val app get() = application as DigitalSignageApplication

    override fun initView() {
        // 加载当前设置
        lifecycleScope.launch {
            val config = app.signageManager.configStore.configFlow.first()
            binding.etPort.setText(config.port.toString())
            binding.etDeviceName.setText(config.deviceName)
            binding.switchAutoStart.isChecked = config.autoStart
            binding.switchAutoRestore.isChecked = config.autoRestore
        }

        binding.btnSave.setOnClickListener {
            saveSettings {
                Toast.makeText(this, "配置保存成功", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.btnRestartServer.setOnClickListener {
            saveSettings {
                lifecycleScope.launch(Dispatchers.IO) {
                    val config = app.signageManager.configStore.configFlow.first()
                    try {
                        app.httpServer.stop()
                        app.httpServer.start(config.port)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsActivity, "配置保存成功，且 HTTP 服务已在端口 ${config.port} 重启", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsActivity, "服务重启失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun initData() = Unit

    private fun saveSettings(onComplete: () -> Unit) {
        val portStr = binding.etPort.text.toString()
        val port = portStr.toIntOrNull() ?: 8080
        val deviceName = binding.etDeviceName.text.toString()
        val autoStart = binding.switchAutoStart.isChecked
        val autoRestore = binding.switchAutoRestore.isChecked

        val config = SignageConfig(
            port = port,
            deviceName = deviceName,
            autoStart = autoStart,
            autoRestore = autoRestore
        )

        lifecycleScope.launch {
            app.signageManager.configStore.saveConfig(config)
            onComplete()
        }
    }
}
