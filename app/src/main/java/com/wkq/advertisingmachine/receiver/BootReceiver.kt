package com.wkq.advertisingmachine.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wkq.advertisingmachine.MainActivity
import com.wkq.advertisingmachine.data.ConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val configStore = ConfigStore(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val config = configStore.configFlow.first()
                if (config.autoStart) {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(launchIntent)
                }
            }
        }
    }
}
