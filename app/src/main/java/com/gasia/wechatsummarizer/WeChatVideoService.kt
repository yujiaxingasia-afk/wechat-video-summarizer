package com.gasia.wechatsummarizer

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class WeChatVideoService : AccessibilityService() {

    companion object {
        private const val TAG = "WeChatVideoService"
        private const val OPENCLAW_API = "http://192.168.1.100:18789/api/summarize" // 替换为你的OpenClaw地址
        private var isRunning = false
        private var lastVideoTitle = ""
        private var capturedTexts = mutableListOf<String>()
        private val client = OkHttpClient()
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected")
        Toast.makeText(this, "微信视频总结服务已启动", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isRunning) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val className = event.className?.toString() ?: ""
                Log.d(TAG, "Window changed: $className")
                
                // 检测是否在视频号界面
                if (className.contains("Finder") || className.contains("VideoPlayer")) {
                    Log.d(TAG, "Detected video player")
                    captureVideoInfo()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (isRunning && event.source != null) {
                    extractTextFromNode(event.source)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    private fun captureVideoInfo() {
        val rootNode = rootInActiveWindow ?: return
        
        // 尝试获取视频标题
        val titleNode = findNodeByText(rootNode, listOf("标题", "title", "textView"))
        val title = titleNode?.text?.toString() ?: ""
        
        if (title.isNotEmpty() && title != lastVideoTitle) {
            lastVideoTitle = title
            Log.d(TAG, "Video title: $title")
            capturedTexts.add("[标题] $title")
        }
        
        // 获取视频描述/字幕
        extractTextFromNode(rootNode)
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo) {
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && text.length > 2) {
            if (!capturedTexts.contains(text)) {
                capturedTexts.add(text)
                Log.d(TAG, "Captured text: $text")
            }
        }
        
        // 递归遍历子节点
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { extractTextFromNode(it) }
        }
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, keywords: List<String>): AccessibilityNodeInfo? {
        if (node.text != null) {
            val text = node.text.toString().lowercase()
            for (keyword in keywords) {
                if (text.contains(keyword.lowercase())) {
                    return node
                }
            }
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findNodeByText(child, keywords)?.let { return it }
            }
        }
        return null
    }

    fun startCapture() {
        isRunning = true
        capturedTexts.clear()
        lastVideoTitle = ""
        Toast.makeText(this, "开始捕获视频内容...", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Started capturing")
    }

    fun stopAndSummarize() {
        isRunning = false
        Log.d(TAG, "Stopped capturing, texts: ${capturedTexts.size}")
        
        if (capturedTexts.isEmpty()) {
            Toast.makeText(this, "没有捕获到内容", Toast.LENGTH_SHORT).show()
            return
        }
        
        val content = capturedTexts.joinToString("\n")
        sendToOpenClaw(content)
    }

    private fun sendToOpenClaw(content: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val json = """
                    {
                        "action": "summarize_video",
                        "content": ${com.google.gson.Gson().toJson(content)}
                    }
                """.trimIndent()
                
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(OPENCLAW_API)
                    .post(body)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@WeChatVideoService, "已发送到OpenClaw进行总结", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@WeChatVideoService, "发送失败: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to OpenClaw", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WeChatVideoService, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        fun start() {
            isRunning = true
        }
        
        fun stop() {
            isRunning = false
        }
        
        fun isServiceRunning() = isRunning
    }
}