# Android Digital Signage Function Specification

Version: v1.0

---

# 1. Project Overview

开发一个 Android 广告机播放器。

Android App 本身作为 HTTP Server。

无需 JNI。

无需 SO。

无需 C++。

所有功能均使用 Kotlin 实现。

控制端通过 HTTP API 控制广告机。

支持局域网控制。

支持多个显示器。

支持播放状态恢复。

---

# 2. Startup

应用启动后：

- 显示灰色主页面
- 自动启动 HTTP Server
- 自动获取当前设备 IP
- 默认监听 0.0.0.0
- 默认端口 8080
- 自动读取保存配置
- 自动恢复所有窗口
- 自动恢复所有播放器

页面显示：

Server Running

192.168.x.x:8080

长按页面：

打开设置页面。

---

# 3. Settings

支持配置：

- Server Port
- Device Name
- Auto Start
- Auto Restore
- Save
- Restart Server

---

# 4. HTTP Server

Android App 本身作为 HTTP Server。

默认：

Port：8080

监听：

0.0.0.0

局域网所有设备均可访问。

例如：

http://192.168.1.100:8080/api

支持：

GET

POST

统一返回 JSON。

---

# 5. Local IP

启动后自动获取：

当前 WiFi IP

例如：

192.168.1.100

自动显示：

192.168.1.100:8080

无需用户配置。

---

# 6. Multi Display

支持：

多个 HDMI

USB Display

扩展屏

自动检测显示器数量。

每块显示器：

独立管理。

互不影响。

支持动态插拔。

---

# 7. Window

每块显示器支持多个窗口。

窗口支持：

创建

删除

修改

移动

缩放

显示

隐藏

背景颜色

Layer

多个窗口允许重叠。

Layer 越大越靠上。

---

# 8. Coordinate System

所有显示器统一逻辑坐标：

1920 × 1920

自动缩放到真实分辨率。

---

# 9. Supported Media

支持：

图片

视频

直播流

本地资源

网络资源

---

# 10. Image

支持：

PNG

JPG

JPEG

WEBP

GIF（可选）

支持：

HTTP

HTTPS

本地文件

自动缩放。

---

# 11. Video

支持：

MP4

MOV

MKV

AVI

M3U8

HTTP

HTTPS

支持循环播放。

支持停止播放。

支持切换资源。

---

# 12. Live Stream

支持：

RTSP

H264

H265

TCP

UDP

支持自动重连。

适用于：

监控

直播

IPC 摄像头

NVR

---

# 13. Local Resource

支持访问：

file://

content://

/storage/emulated/0/

/sdcard/

/Movies/

/Pictures/

/Download/

/Android/media/

控制端可以直接播放本机资源。

例如：

/storage/emulated/0/Movies/demo.mp4

---

# 14. Remote Resource

支持：

HTTP

HTTPS

RTSP

M3U8

控制端直接传入 URL 即可播放。

---

# 15. Overlay Text

支持：

添加文字

删除文字

清空文字

多行显示

颜色

字号

背景

位置

最新添加的文字显示在第一行。

---

# 16. Background Music

支持：

MP3

AAC

WAV

HTTP

HTTPS

支持：

开始播放

停止播放

循环播放

---

# 17. State Persistence

自动保存：

窗口

播放器

播放资源

文字

背景音乐

设置

应用重启后：

自动恢复所有内容。

无需重新下发命令。

---

# 18. File Browser

支持浏览本机资源。

例如：

Movies

Pictures

Download

DCIM

Android/media

控制端可获取目录内容。

---

# 19. Screenshot

支持当前画面截图。

支持指定显示器截图。

返回 PNG。

便于远程查看播放效果。

---

# 20. HTTP API

支持：

- ping
- health
- version
- getdispchan
- getscreen
- setscreen
- modifyscreen
- deletescreen
- startplay
- stopplay
- addtext
- cleartext
- playsound
- stopplaysound
- listfiles
- snapshot

统一：

JSON Request

JSON Response

---

# 21. Control Client

支持：

Windows

Android

Linux

Web

统一使用 HTTP JSON 协议。

---

# 22. Future Features

后续可扩展：

- HTML 页面
- WebView
- PDF
- Office 文档
- GIF 动画
- 定时播放
- 播放列表
- FTP 更新
- OTA 更新
- 心跳检测
- 远程日志
- 远程重启
- 多设备管理

---

# 23. Development Notes

本文件仅描述产品功能。

具体实现请参考：

- Android 开发规范
- Kotlin 开发规范
- MVVM 规范
- Media3 规范
- HTTP Server 规范

所有实现遵循上述开发规范。