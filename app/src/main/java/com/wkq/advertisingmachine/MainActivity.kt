package com.wkq.advertisingmachine

import android.widget.Toast
import com.wkq.advertisingmachine.databinding.ActivityMainBinding
import com.wkq.base.activity.BaseActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun initView() {
        binding.tvServerStatus.text = getString(R.string.server_running)
        binding.tvServerAddress.text = getString(R.string.default_server_address)
        binding.root.setOnLongClickListener {
            Toast.makeText(this, R.string.settings_entry_todo, Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun initData() = Unit
}
