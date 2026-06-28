package com.wkq.advertisingmachine.control

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.wkq.advertisingmachine.data.ConfigStore
import com.wkq.advertisingmachine.display.DisplayChannelManager
import com.wkq.advertisingmachine.model.DisplayChannelState
import com.wkq.advertisingmachine.model.MediaState
import com.wkq.advertisingmachine.model.ScreenWindow
import com.wkq.advertisingmachine.model.SignageConfig
import com.wkq.advertisingmachine.model.SignageState
import com.wkq.advertisingmachine.model.TextState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

class SignageManager(private val context: Context) {

    val displayChannelManager = DisplayChannelManager(context)
    val configStore = ConfigStore(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private var mediaPlayer: MediaPlayer? = null
    private var bgMusicUrl: String? = null
    private var isBgMusicLooping: Boolean = true
    private var isBgMusicPlaying: Boolean = false

    init {
        // 自动恢复状态
        scope.launch {
            // 稍作等待以使首个物理 Display 初始化完成
            delay(1000)
            restoreSignageState()
        }
    }

    // --- 状态恢复 ---

    private suspend fun restoreSignageState() {
        val config = configStore.configFlow.firstOrNull() ?: SignageConfig()
        if (!config.autoRestore) return

        val state = configStore.stateFlow.firstOrNull() ?: return

        // 1. 恢复背景音乐
        if (state.isBgMusicPlaying && !state.bgMusicUrl.isNullOrEmpty()) {
            launchOnMain {
                startBgMusic(state.bgMusicUrl, state.isBgMusicLooping)
            }
        }

        // 2. 恢复各屏幕上的窗口和内容
        state.displayStates.forEach { channelState ->
            channelState.windows.forEach { window ->
                // 重建窗口
                displayChannelManager.addOrUpdateWindow(
                    displayId = channelState.displayId,
                    screenId = window.screenId,
                    x = window.x,
                    y = window.y,
                    w = window.w,
                    h = window.h,
                    layer = window.layer,
                    bgColor = window.bgColor
                )

                // 重建媒体播放
                val media = window.currentMedia
                if (media != null) {
                    // 等待窗口在 UI 线程创建后再下发播放 (给一些主线程切换延迟)
                    delay(100)
                    displayChannelManager.playMedia(
                        displayId = channelState.displayId,
                        screenId = window.screenId,
                        mediaUrl = media.mediaUrl,
                        mediaType = media.mediaType,
                        isLooping = media.isLooping
                    )
                }

                // 重建字幕
                window.overlayTextList.forEach { textState ->
                    delay(50)
                    displayChannelManager.addOverlayText(
                        displayId = channelState.displayId,
                        screenId = window.screenId,
                        textState = textState
                    )
                }
            }
        }
    }

    // --- 自动存盘逻辑 ---

    fun saveCurrentStateAsync() {
        scope.launch {
            // 等待 UI 操作渲染完毕再抓取状态
            delay(300)
            val displayStates = displayChannelManager.getDisplayChannelsState()
            val state = SignageState(
                displayStates = displayStates,
                bgMusicUrl = bgMusicUrl,
                isBgMusicLooping = isBgMusicLooping,
                isBgMusicPlaying = isBgMusicPlaying
            )
            configStore.saveState(state)
        }
    }

    // --- 业务控制层 ---

    fun addOrUpdateWindow(
        displayId: Int,
        screenId: String,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        layer: Int,
        bgColor: String
    ): Boolean {
        val ok = displayChannelManager.addOrUpdateWindow(displayId, screenId, x, y, w, h, layer, bgColor)
        if (ok) saveCurrentStateAsync()
        return ok
    }

    fun removeWindow(displayId: Int, screenId: String): Boolean {
        val ok = displayChannelManager.removeWindow(displayId, screenId)
        if (ok) saveCurrentStateAsync()
        return ok
    }

    fun playMedia(
        displayId: Int,
        screenId: String,
        mediaUrl: String,
        mediaType: String,
        isLooping: Boolean
    ): Boolean {
        val ok = displayChannelManager.playMedia(displayId, screenId, mediaUrl, mediaType, isLooping)
        if (ok) saveCurrentStateAsync()
        return ok
    }

    fun stopMedia(displayId: Int, screenId: String): Boolean {
        val ok = displayChannelManager.stopMedia(displayId, screenId)
        if (ok) saveCurrentStateAsync()
        return ok
    }

    fun addOverlayText(
        displayId: Int,
        screenId: String,
        textState: TextState
    ): Boolean {
        val ok = displayChannelManager.addOverlayText(displayId, screenId, textState)
        if (ok) saveCurrentStateAsync()
        return ok
    }

    fun removeOverlayText(displayId: Int, screenId: String, textId: String): Boolean {
        val ok = displayChannelManager.removeOverlayText(displayId, screenId, textId)
        if (ok) saveCurrentStateAsync()
        return ok
    }

    fun clearOverlayText(displayId: Int, screenId: String): Boolean {
        val ok = displayChannelManager.clearOverlayText(displayId, screenId)
        if (ok) saveCurrentStateAsync()
        return ok
    }

    // --- 背景音乐控制 ---

    fun startBgMusic(url: String, isLooping: Boolean) {
        stopBgMusic()

        this.bgMusicUrl = url
        this.isBgMusicLooping = isLooping
        this.isBgMusicPlaying = true

        try {
            val player = MediaPlayer().apply {
                val cleanUrl = if (url.startsWith("file://")) url.removePrefix("file://") else url
                val uri = if (cleanUrl.startsWith("/")) Uri.fromFile(File(cleanUrl)) else Uri.parse(cleanUrl)
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                this.isLooping = isLooping
                prepare()
                start()
            }
            mediaPlayer = player
            saveCurrentStateAsync()
        } catch (e: Exception) {
            e.printStackTrace()
            isBgMusicPlaying = false
        }
    }

    fun stopBgMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        isBgMusicPlaying = false
        saveCurrentStateAsync()
    }

    fun release() {
        stopBgMusic()
        displayChannelManager.release()
    }

    private fun launchOnMain(block: suspend () -> Unit) {
        scope.launch(Dispatchers.Main) {
            block()
        }
    }
}
