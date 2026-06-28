package com.wkq.advertisingmachine.model

import kotlinx.serialization.Serializable

@Serializable
data class SignageConfig(
    val port: Int = 8080,
    val deviceName: String = "Advertising Machine",
    val autoStart: Boolean = true,
    val autoRestore: Boolean = true
)

@Serializable
data class TextState(
    val id: String,
    val text: String,
    val color: String = "#FFFFFF",
    val size: Float = 24f,
    val bgColor: String = "#00000000",
    val x: Int = 0,
    val y: Int = 0
)

@Serializable
data class MediaState(
    val mediaUrl: String,
    val mediaType: String, // "image", "video", "live"
    val isLooping: Boolean = true
)

@Serializable
data class ScreenWindow(
    val screenId: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val layer: Int = 0,
    val bgColor: String = "#00000000",
    var currentMedia: MediaState? = null,
    val overlayTextList: MutableList<TextState> = mutableListOf()
)

@Serializable
data class DisplayChannelState(
    val displayId: Int,
    val windows: List<ScreenWindow> = emptyList()
)

@Serializable
data class SignageState(
    val displayStates: List<DisplayChannelState> = emptyList(),
    val bgMusicUrl: String? = null,
    val isBgMusicLooping: Boolean = true,
    val isBgMusicPlaying: Boolean = false
)
