package com.wkq.advertisingmachine.http

import android.graphics.Bitmap
import android.os.Environment
import com.wkq.advertisingmachine.control.SignageManager
import com.wkq.advertisingmachine.model.TextState
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.request.receive


import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

@Serializable
data class SimpleResponse(val code: Int, val message: String)

@Serializable
data class VersionResponse(val code: Int, val version: String)

@Serializable
data class DisplaysResponse(val code: Int, val displays: List<Int>)

@Serializable
data class GetScreenResponse(val code: Int, val data: List<com.wkq.advertisingmachine.model.DisplayChannelState>)

@Serializable
data class ScreenRequest(
    val displayId: Int,
    val screenId: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val layer: Int = 0,
    val bgColor: String = "#00000000"
)

@Serializable
data class DeleteScreenRequest(val displayId: Int, val screenId: String)

@Serializable
data class StartPlayRequest(
    val displayId: Int,
    val screenId: String,
    val url: String,
    val type: String, // "image", "video", "live"
    val loop: Boolean = true
)

@Serializable
data class StopPlayRequest(val displayId: Int, val screenId: String)

@Serializable
data class AddTextRequest(
    val displayId: Int,
    val screenId: String,
    val textId: String,
    val text: String,
    val color: String = "#FFFFFF",
    val size: Float = 24f,
    val bgColor: String = "#00000000",
    val x: Int = 0,
    val y: Int = 0
)

@Serializable
data class ClearTextRequest(val displayId: Int, val screenId: String, val textId: String? = null)

@Serializable
data class PlaySoundRequest(val url: String, val loop: Boolean = true)

@Serializable
data class FileItem(val name: String, val isDir: Boolean, val size: Long)

@Serializable
data class ListFilesResponse(val code: Int, val path: String, val files: List<FileItem>)

class SignageHttpServer(private val signageManager: SignageManager) {

    private var server: ApplicationEngine? = null

    fun start(port: Int) {
        if (server != null) return

        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }

            routing {
                intercept(ApplicationCallPipeline.Plugins) {
                    call.response.headers.append("Access-Control-Allow-Origin", "*")
                    call.response.headers.append("Access-Control-Allow-Headers", "*")
                    call.response.headers.append("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE")
                    if (call.request.local.method == HttpMethod.Options) {
                        call.respond(HttpStatusCode.OK)
                        finish()
                    }
                }


                route("/api") {
                    // 1. ping
                    get("/ping") {
                        call.respond(SimpleResponse(200, "pong"))
                    }

                    // 2. health
                    get("/health") {
                        call.respond(SimpleResponse(200, "healthy"))
                    }

                    // 3. version
                    get("/version") {
                        call.respond(VersionResponse(200, "1.0.0"))
                    }

                    // 4. getdispchan (获取显示器ID列表)
                    get("/getdispchan") {
                        val displays = signageManager.displayChannelManager.getConnectedDisplays()
                        call.respond(DisplaysResponse(200, displays))
                    }

                    // 5. getscreen (获取当前所有窗口和状态)
                    get("/getscreen") {
                        val data = signageManager.displayChannelManager.getDisplayChannelsState()
                        call.respond(GetScreenResponse(200, data))
                    }

                    // 6. setscreen (创建/覆盖窗口)
                    post("/setscreen") {
                        val req = call.receive<ScreenRequest>()
                        val ok = signageManager.addOrUpdateWindow(
                            req.displayId, req.screenId, req.x, req.y, req.w, req.h, req.layer, req.bgColor
                        )
                        if (ok) {
                            call.respond(SimpleResponse(200, "success"))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, SimpleResponse(400, "Failed to create/update window"))
                        }
                    }

                    // 7. modifyscreen (修改窗口)
                    post("/modifyscreen") {
                        val req = call.receive<ScreenRequest>()
                        val ok = signageManager.addOrUpdateWindow(
                            req.displayId, req.screenId, req.x, req.y, req.w, req.h, req.layer, req.bgColor
                        )
                        if (ok) {
                            call.respond(SimpleResponse(200, "success"))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, SimpleResponse(400, "Failed to modify window"))
                        }
                    }

                    // 8. deletescreen (删除窗口)
                    post("/deletescreen") {
                        val req = call.receive<DeleteScreenRequest>()
                        val ok = signageManager.removeWindow(req.displayId, req.screenId)
                        if (ok) {
                            call.respond(SimpleResponse(200, "success"))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, SimpleResponse(400, "Failed to delete window"))
                        }
                    }

                    // 9. startplay (开始播放媒体)
                    post("/startplay") {
                        val req = call.receive<StartPlayRequest>()
                        val ok = signageManager.playMedia(
                            req.displayId, req.screenId, req.url, req.type, req.loop
                        )
                        if (ok) {
                            call.respond(SimpleResponse(200, "success"))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, SimpleResponse(400, "Failed to start media playback"))
                        }
                    }

                    // 10. stopplay (停止媒体播放)
                    post("/stopplay") {
                        val req = call.receive<StopPlayRequest>()
                        val ok = signageManager.stopMedia(req.displayId, req.screenId)
                        if (ok) {
                            call.respond(SimpleResponse(200, "success"))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, SimpleResponse(400, "Failed to stop media playback"))
                        }
                    }

                    // 11. addtext (添加叠加文本)
                    post("/addtext") {
                        val req = call.receive<AddTextRequest>()
                        val textState = TextState(
                            id = req.textId,
                            text = req.text,
                            color = req.color,
                            size = req.size,
                            bgColor = req.bgColor,
                            x = req.x,
                            y = req.y
                        )
                        val ok = signageManager.addOverlayText(req.displayId, req.screenId, textState)
                        if (ok) {
                            call.respond(SimpleResponse(200, "success"))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, SimpleResponse(400, "Failed to add text"))
                        }
                    }

                    // 12. cleartext (清理/删除文字)
                    post("/cleartext") {
                        val req = call.receive<ClearTextRequest>()
                        val ok = if (!req.textId.isNullOrEmpty()) {
                            signageManager.removeOverlayText(req.displayId, req.screenId, req.textId)
                        } else {
                            signageManager.clearOverlayText(req.displayId, req.screenId)
                        }
                        if (ok) {
                            call.respond(SimpleResponse(200, "success"))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, SimpleResponse(400, "Failed to clear text"))
                        }
                    }

                    // 13. playsound (背景音乐)
                    post("/playsound") {
                        val req = call.receive<PlaySoundRequest>()
                        signageManager.startBgMusic(req.url, req.loop)
                        call.respond(SimpleResponse(200, "success"))
                    }

                    // 14. stopplaysound (停止背景音乐)
                    post("/stopplaysound") {
                        signageManager.stopBgMusic()
                        call.respond(SimpleResponse(200, "success"))
                    }

                    // 15. listfiles (列出本机目录下的文件)
                    get("/listfiles") {
                        val pathParam = call.request.queryParameters["path"]
                        val targetPath = if (pathParam.isNullOrEmpty()) {
                            Environment.getExternalStorageDirectory().absolutePath
                        } else {
                            pathParam
                        }

                        val file = File(targetPath)
                        if (!file.exists() || !file.isDirectory) {
                            call.respond(HttpStatusCode.NotFound, SimpleResponse(404, "Directory not found"))
                            return@get
                        }

                        val items = withContext(Dispatchers.IO) {
                            file.listFiles()?.map { f ->
                                FileItem(
                                    name = f.name,
                                    isDir = f.isDirectory,
                                    size = if (f.isDirectory) 0 else f.length()
                                )
                            }?.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() })) ?: emptyList()
                        }
                        call.respond(ListFilesResponse(200, targetPath, items))
                    }

                    // 16. snapshot (画面截图，直接返回PNG二进制流)
                    get("/snapshot") {
                        val displayIdParam = call.request.queryParameters["displayId"]
                        val displayId = displayIdParam?.toIntOrNull() ?: 0

                        val bitmap = suspendCancellableCoroutine<Bitmap?> { continuation ->
                            signageManager.displayChannelManager.takeScreenshot(displayId) { bmp ->
                                if (continuation.isActive) {
                                    continuation.resume(bmp)
                                }
                            }
                        }

                        if (bitmap != null) {
                            val stream = ByteArrayOutputStream()
                            withContext(Dispatchers.IO) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            }
                            val bytes = stream.toByteArray()
                            call.respondBytes(bytes, ContentType.Image.PNG)
                            bitmap.recycle()
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, SimpleResponse(500, "Failed to capture screenshot"))
                        }
                    }
                }
            }
        }

        server?.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
}
