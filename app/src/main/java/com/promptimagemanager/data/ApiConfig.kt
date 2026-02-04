package com.promptimagemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * API配置实体类
 * 支持多种API类型：文字聊天、图片反推、图片生成、视频生成
 */
@Entity(tableName = "api_configs")
data class ApiConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,           // 配置名称（如"OpenAI GPT-4"）
    val apiType: Int,           // API类型：1=文字聊天, 2=图片反推, 3=图片生成, 4=视频生成
    val baseUrl: String,        // API基础URL
    val apiKey: String,         // API密钥
    val model: String,          // 模型名称
    val headers: String = "",   // 额外请求头（JSON格式）
    val params: String = "",    // 额外参数（JSON格式）
    val timeout: Int = 60,      // 超时时间（秒）
    val isDefault: Boolean = false,  // 是否为默认配置
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_CHAT = 1        // 文字聊天
        const val TYPE_IMAGE_TO_TEXT = 2  // 图片反推
        const val TYPE_TEXT_TO_IMAGE = 3  // 文生图
        const val TYPE_VIDEO = 4       // 视频生成
        
        fun getTypeName(type: Int): String {
            return when (type) {
                TYPE_CHAT -> "文字聊天"
                TYPE_IMAGE_TO_TEXT -> "图片反推"
                TYPE_TEXT_TO_IMAGE -> "图片生成"
                TYPE_VIDEO -> "视频生成"
                else -> "未知"
            }
        }
    }
}

/**
 * 预设API配置模板
 */
object ApiPresets {
    
    // OpenAI GPT
    fun openAI(apiKey: String) = ApiConfig(
        name = "OpenAI GPT-4",
        apiType = ApiConfig.TYPE_CHAT,
        baseUrl = "https://api.openai.com/v1/chat/completions",
        apiKey = apiKey,
        model = "gpt-4",
        headers = "{\"Content-Type\": \"application/json\"}"
    )
    
    // Claude
    fun claude(apiKey: String) = ApiConfig(
        name = "Claude 3",
        apiType = ApiConfig.TYPE_CHAT,
        baseUrl = "https://api.anthropic.com/v1/messages",
        apiKey = apiKey,
        model = "claude-3-opus-20240229",
        headers = "{\"Content-Type\": \"application/json\", \"anthropic-version\": \"2023-06-01\"}"
    )
    
    // comfly.chat (用户指定的网站)
    fun comflyChat(apiKey: String) = ApiConfig(
        name = "Comfly Chat",
        apiType = ApiConfig.TYPE_CHAT,
        baseUrl = "https://ai.comfly.chat/v1/chat/completions",
        apiKey = apiKey,
        model = "gpt-4",
        headers = "{\"Content-Type\": \"application/json\"}"
    )
    
    // 图片反推 - Replicate BLIP
    fun replicateBlip(apiKey: String) = ApiConfig(
        name = "Replicate BLIP",
        apiType = ApiConfig.TYPE_IMAGE_TO_TEXT,
        baseUrl = "https://api.replicate.com/v1/predictions",
        apiKey = apiKey,
        model = "salesforce/blip:2e1dddc8621f72155f24cf2e0adbde548458d3cab9f00c0139eea840d0ac4746",
        headers = "{\"Content-Type\": \"application/json\"}"
    )
    
    // 图片反推 - OpenAI Vision
    fun openAIVision(apiKey: String) = ApiConfig(
        name = "OpenAI Vision",
        apiType = ApiConfig.TYPE_IMAGE_TO_TEXT,
        baseUrl = "https://api.openai.com/v1/chat/completions",
        apiKey = apiKey,
        model = "gpt-4-vision-preview",
        headers = "{\"Content-Type\": \"application/json\"}"
    )
    
    // 文生图 - DALL-E 3
    fun dalle3(apiKey: String) = ApiConfig(
        name = "DALL-E 3",
        apiType = ApiConfig.TYPE_TEXT_TO_IMAGE,
        baseUrl = "https://api.openai.com/v1/images/generations",
        apiKey = apiKey,
        model = "dall-e-3",
        headers = "{\"Content-Type\": \"application/json\"}"
    )
    
    // 文生图 - Stability AI
    fun stabilityAI(apiKey: String) = ApiConfig(
        name = "Stability AI",
        apiType = ApiConfig.TYPE_TEXT_TO_IMAGE,
        baseUrl = "https://api.stability.ai/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image",
        apiKey = apiKey,
        model = "stable-diffusion-xl-1024-v1-0",
        headers = "{\"Content-Type\": \"application/json\", \"Accept\": \"application/json\"}"
    )
    
    // 文生图 - 香蕉模型 (Banana.dev)
    fun bananaDev(apiKey: String, modelKey: String) = ApiConfig(
        name = "Banana.dev",
        apiType = ApiConfig.TYPE_TEXT_TO_IMAGE,
        baseUrl = "https://api.banana.dev/start/$modelKey",
        apiKey = apiKey,
        model = modelKey,
        headers = "{\"Content-Type\": \"application/json\"}"
    )
    
    // 视频生成 - Runway Gen-2
    fun runwayGen2(apiKey: String) = ApiConfig(
        name = "Runway Gen-2",
        apiType = ApiConfig.TYPE_VIDEO,
        baseUrl = "https://api.runwayml.com/v1/generations",
        apiKey = apiKey,
        model = "gen2",
        headers = "{\"Content-Type\": \"application/json\"}"
    )
    
    // 视频生成 - Pika Labs
    fun pikaLabs(apiKey: String) = ApiConfig(
        name = "Pika Labs",
        apiType = ApiConfig.TYPE_VIDEO,
        baseUrl = "https://api.pika.art/generations",
        apiKey = apiKey,
        model = "pika",
        headers = "{\"Content-Type\": \"application/json\"}"
    )
    
    // 视频生成 - Replicate
    fun replicateVideo(apiKey: String) = ApiConfig(
        name = "Replicate Video",
        apiType = ApiConfig.TYPE_VIDEO,
        baseUrl = "https://api.replicate.com/v1/predictions",
        apiKey = apiKey,
        model = "stability-ai/stable-video-diffusion",
        headers = "{\"Content-Type\": \"application/json\"}"
    )
    
    // 获取所有预设
    fun getAllPresets(apiKey: String = "your-api-key"): List<ApiConfig> {
        return listOf(
            openAI(apiKey),
            claude(apiKey),
            comflyChat(apiKey),
            replicateBlip(apiKey),
            openAIVision(apiKey),
            dalle3(apiKey),
            stabilityAI(apiKey),
            bananaDev(apiKey, "your-model-key"),
            runwayGen2(apiKey),
            pikaLabs(apiKey),
            replicateVideo(apiKey)
        )
    }
}
