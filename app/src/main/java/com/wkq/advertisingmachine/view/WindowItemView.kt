package com.wkq.advertisingmachine.view

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.load
import com.wkq.advertisingmachine.model.MediaState
import com.wkq.advertisingmachine.model.TextState
import java.io.File

class WindowItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var screenId: String = ""
        internal set

    private val imageView: ImageView
    private val playerView: PlayerView
    private val textContainer: LinearLayout

    private var player: ExoPlayer? = null
    private var currentMedia: MediaState? = null
    private val textStates = mutableMapOf<String, TextState>()

    val currentMediaState: MediaState? get() = currentMedia

    init {
        // 1. 初始化图片组件
        imageView = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_XY
            visibility = View.GONE
        }
        addView(imageView)

        // 2. 初始化视频播放组件
        playerView = PlayerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            useController = false
            visibility = View.GONE
        }
        addView(playerView)

        // 3. 初始化文本叠加区域（默认新添加的在最上方）
        textContainer = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        addView(textContainer)
    }

    @OptIn(UnstableApi::class)
    fun playMedia(mediaUrl: String, mediaType: String, isLooping: Boolean) {
        currentMedia = MediaState(mediaUrl, mediaType, isLooping)

        when (mediaType.lowercase()) {
            "image" -> {
                stopVideo()
                imageView.visibility = View.VISIBLE
                playerView.visibility = View.GONE

                val imageFile = File(mediaUrl.removePrefix("file://"))
                if (imageFile.exists()) {
                    imageView.load(imageFile)
                } else {
                    imageView.load(mediaUrl)
                }
            }
            "video", "live" -> {
                imageView.visibility = View.GONE
                playerView.visibility = View.VISIBLE
                initAndStartVideo(mediaUrl, isLooping)
            }
        }
    }

    fun stopMedia() {
        currentMedia = null
        imageView.visibility = View.GONE
        imageView.setImageDrawable(null)
        stopVideo()
        playerView.visibility = View.GONE
    }

    @OptIn(UnstableApi::class)
    private fun initAndStartVideo(mediaUrl: String, isLooping: Boolean) {
        stopVideo()

        val newPlayer = ExoPlayer.Builder(context).build().apply {
            repeatMode = if (isLooping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        }
        player = newPlayer
        playerView.player = newPlayer

        val uri = if (mediaUrl.startsWith("/") || mediaUrl.startsWith("file://")) {
            val cleanPath = mediaUrl.removePrefix("file://")
            Uri.fromFile(File(cleanPath))
        } else {
            Uri.parse(mediaUrl)
        }

        val mediaItem = MediaItem.fromUri(uri)
        newPlayer.setMediaItem(mediaItem)
        newPlayer.prepare()
        newPlayer.playWhenReady = true
    }

    private fun stopVideo() {
        playerView.player = null
        player?.let {
            it.stop()
            it.release()
        }
        player = null
    }

    fun addOverlayText(textState: TextState) {
        // 先删除已存在的相同 id 文本
        removeOverlayText(textState.id)
        textStates[textState.id] = textState

        val tv = TextView(context).apply {
            tag = textState.id
            text = textState.text
            textSize = textState.size
            setTextColor(parseColorOrDefault(textState.color, Color.WHITE))
            setBackgroundColor(parseColorOrDefault(textState.bgColor, Color.TRANSPARENT))
        }

        if (textState.x > 0 || textState.y > 0) {
            // 如果指定了具体坐标，则作为 FrameLayout 的绝对子 View 添加
            val scaleX = width.toFloat() / 1920f
            val scaleY = height.toFloat() / 1920f
            val realLeft = if (scaleX > 0) (textState.x * scaleX).toInt() else textState.x
            val realTop = if (scaleY > 0) (textState.y * scaleY).toInt() else textState.y
            val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = realLeft
                topMargin = realTop
            }
            addView(tv, lp)
        } else {
            // 默认新添加的置顶（第一行）
            textContainer.addView(tv, 0)
        }
    }

    fun removeOverlayText(textId: String) {
        textStates.remove(textId)
        // 查找 LinearLayout 里的 View
        val viewInContainer = textContainer.findViewWithTag<View>(textId)
        if (viewInContainer != null) {
            textContainer.removeView(viewInContainer)
        }
        // 查找 FrameLayout 绝对定位里的 View
        val viewInRoot = findViewWithTag<View>(textId)
        if (viewInRoot != null && viewInRoot != textContainer && viewInRoot != imageView && viewInRoot != playerView) {
            removeView(viewInRoot)
        }
    }

    fun clearOverlayTexts() {
        textStates.clear()
        textContainer.removeAllViews()
        // 清理 FrameLayout 绝对定位的 View
        val viewsToRemove = mutableListOf<View>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child != textContainer && child != imageView && child != playerView) {
                viewsToRemove.add(child)
            }
        }
        viewsToRemove.forEach { removeView(it) }
    }

    fun getOverlayTexts(): MutableList<TextState> {
        return textStates.values.toMutableList()
    }

    fun getBackgroundColorString(): String {
        val background = background
        if (background is android.graphics.drawable.ColorDrawable) {
            val color = background.color
            return String.format("#%08X", color)
        }
        return "#00000000"
    }

    fun release() {
        stopVideo()
        imageView.setImageDrawable(null)
        clearOverlayTexts()
    }

    private fun parseColorOrDefault(colorStr: String, defaultColor: Int): Int {
        return try {
            Color.parseColor(colorStr)
        } catch (e: Exception) {
            defaultColor
        }
    }
}
