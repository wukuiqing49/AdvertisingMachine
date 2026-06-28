package com.wkq.advertisingmachine.display

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.PixelCopy
import com.wkq.advertisingmachine.model.DisplayChannelState
import com.wkq.advertisingmachine.model.TextState
import com.wkq.advertisingmachine.view.LogicalViewportContainer
import java.util.concurrent.ConcurrentHashMap

class DisplayChannel(
    val displayId: Int,
    val display: Display,
    var presentation: DisplayPresentation? = null,
    var mainContainer: LogicalViewportContainer? = null
) {
    val container: LogicalViewportContainer?
        get() = mainContainer ?: presentation?.container
}

class DisplayChannelManager(private val context: Context) : DisplayManager.DisplayListener {

    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val channels = ConcurrentHashMap<Int, DisplayChannel>()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        displayManager.registerDisplayListener(this, mainHandler)
        initializeConnectedDisplays()
    }

    private fun initializeConnectedDisplays() {
        val currentDisplays = displayManager.displays
        for (display in currentDisplays) {
            val displayId = display.displayId
            if (displayId == Display.DEFAULT_DISPLAY) {
                // 主屏，等待 registerMainContainer 注册
                channels[displayId] = DisplayChannel(displayId, display)
            } else {
                // 副屏，动态创建 Presentation
                mainHandler.post {
                    createAndShowPresentation(display)
                }
            }
        }
    }

    private fun createAndShowPresentation(display: Display) {
        try {
            val presentation = DisplayPresentation(context, display)
            presentation.show()
            channels[display.displayId] = DisplayChannel(display.displayId, display, presentation)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun registerMainContainer(container: LogicalViewportContainer) {
        val defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        if (defaultDisplay != null) {
            val channel = channels[Display.DEFAULT_DISPLAY]
            if (channel != null) {
                channel.mainContainer = container
            } else {
                channels[Display.DEFAULT_DISPLAY] = DisplayChannel(
                    Display.DEFAULT_DISPLAY,
                    defaultDisplay,
                    mainContainer = container
                )
            }
        }
    }

    // --- DisplayListener 接口实现 ---

    override fun onDisplayAdded(displayId: Int) {
        if (displayId == Display.DEFAULT_DISPLAY) return
        val display = displayManager.getDisplay(displayId) ?: return
        mainHandler.post {
            createAndShowPresentation(display)
        }
    }

    override fun onDisplayRemoved(displayId: Int) {
        val channel = channels.remove(displayId)
        mainHandler.post {
            channel?.presentation?.dismiss()
        }
    }

    override fun onDisplayChanged(displayId: Int) {
        // 分辨率变化等动态调整
    }

    // --- 业务层接口包装 ---

    fun getConnectedDisplays(): List<Int> {
        return channels.keys.toList().sorted()
    }

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
        val channel = channels[displayId] ?: return false
        val container = channel.container ?: return false
        mainHandler.post {
            container.addOrUpdateWindow(screenId, x, y, w, h, layer, bgColor)
        }
        return true
    }

    fun removeWindow(displayId: Int, screenId: String): Boolean {
        val channel = channels[displayId] ?: return false
        val container = channel.container ?: return false
        mainHandler.post {
            container.removeWindow(screenId)
        }
        return true
    }

    fun playMedia(
        displayId: Int,
        screenId: String,
        mediaUrl: String,
        mediaType: String,
        isLooping: Boolean
    ): Boolean {
        val channel = channels[displayId] ?: return false
        val container = channel.container ?: return false
        mainHandler.post {
            val window = container.findWindowView(screenId)
            window?.playMedia(mediaUrl, mediaType, isLooping)
        }
        return true
    }

    fun stopMedia(displayId: Int, screenId: String): Boolean {
        val channel = channels[displayId] ?: return false
        val container = channel.container ?: return false
        mainHandler.post {
            val window = container.findWindowView(screenId)
            window?.stopMedia()
        }
        return true
    }

    fun addOverlayText(
        displayId: Int,
        screenId: String,
        textState: TextState
    ): Boolean {
        val channel = channels[displayId] ?: return false
        val container = channel.container ?: return false
        mainHandler.post {
            val window = container.findWindowView(screenId)
            window?.addOverlayText(textState)
        }
        return true
    }

    fun removeOverlayText(displayId: Int, screenId: String, textId: String): Boolean {
        val channel = channels[displayId] ?: return false
        val container = channel.container ?: return false
        mainHandler.post {
            val window = container.findWindowView(screenId)
            window?.removeOverlayText(textId)
        }
        return true
    }

    fun clearOverlayText(displayId: Int, screenId: String): Boolean {
        val channel = channels[displayId] ?: return false
        val container = channel.container ?: return false
        mainHandler.post {
            val window = container.findWindowView(screenId)
            window?.clearOverlayTexts()
        }
        return true
    }

    fun clearAllWindows() {
        mainHandler.post {
            channels.values.forEach { channel ->
                channel.container?.clearAllWindows()
            }
        }
    }

    fun getDisplayChannelsState(): List<DisplayChannelState> {
        val list = mutableListOf<DisplayChannelState>()
        channels.values.forEach { channel ->
            val container = channel.container
            if (container != null) {
                list.add(
                    DisplayChannelState(
                        displayId = channel.displayId,
                        windows = container.getAllWindowsData()
                    )
                )
            }
        }
        return list
    }

    fun takeScreenshot(displayId: Int, callback: (Bitmap?) -> Unit) {
        val channel = channels[displayId]
        if (channel == null) {
            callback(null)
            return
        }

        val window = channel.presentation?.window
            ?: (channel.mainContainer?.context as? Activity)?.window

        if (window == null) {
            // 没有 Window 的 fallback：在主线程直接通过 Canvas 绘制 Container View
            mainHandler.post {
                val view = channel.container
                if (view == null) {
                    callback(null)
                } else {
                    try {
                        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        view.draw(canvas)
                        callback(bitmap)
                    } catch (e: Exception) {
                        callback(null)
                    }
                }
            }
            return
        }

        mainHandler.post {
            try {
                val view = window.decorView
                if (view.width <= 0 || view.height <= 0) {
                    callback(null)
                    return@post
                }
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(window, bitmap, { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        callback(bitmap)
                    } else {
                        // PixelCopy 失败后使用 view.draw 兜底
                        try {
                            val canvas = Canvas(bitmap)
                            view.draw(canvas)
                            callback(bitmap)
                        } catch (e: Exception) {
                            callback(null)
                        }
                    }
                }, mainHandler)
            } catch (e: Exception) {
                callback(null)
            }
        }
    }

    fun release() {
        displayManager.unregisterDisplayListener(this)
        mainHandler.post {
            channels.values.forEach { channel ->
                channel.presentation?.dismiss()
                channel.container?.clearAllWindows()
            }
            channels.clear()
        }
    }
}
