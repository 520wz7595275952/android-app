package com.promptimagemanager.api

import android.content.Context
import android.graphics.Bitmap
nimport android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.promptimagemanager.data.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * API服务类 - 处理各种AI API调用
 */
class ApiService(private val context: Context) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    companion object {
        const val TAG = "ApiService"
        
        // 通用结果类
        data class ApiResult<T>(
            val success: Boolean,
            val data: T? = null,
            val error: String? = null
        )
        
        // 聊天消息
        data class ChatMessage(
            val role: String,
            val content: String
        )
        
        // 图片生成结果
        data class ImageGenerationResult(
            val imageUrl: String? = null,
            val imageBase64: String? = null,
            val localPath: String? = null
        )
        
        // 视频生成结果
        data class VideoGenerationResult(
            val videoUrl: String? = null,
            val localPath: String? = null,
            val status: String = "pending"
        )
    }
    
    /**
     * 文字聊天API调用
     */
    suspend fun chat(
        config: ApiConfig,
        messages: List<ChatMessage>,
        temperature: Double = 0.7,
        maxTokens: Int = 2000
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = when {
                config.baseUrl.contains("anthropic") -> {
                    // Claude API格式
                    JSONObject().apply {
                        put("model", config.model)
                        put("max_tokens", maxTokens)
                        put("messages", JSONArray().apply {
                            messages.forEach { msg ->
                                put(JSONObject().apply {
                                    put("role", msg.role)
                                    put("content", msg.content)
                                })
                            }
                        })
                    }
                }
                else -> {
                    // OpenAI格式 (包括comfly.chat)
                    JSONObject().apply {
                        put("model", config.model)
                        put("temperature", temperature)
                        put("max_tokens", maxTokens)
                        put("messages", JSONArray().apply {
                            messages.forEach { msg ->
                                put(JSONObject().apply {
                                    put("role", msg.role)
                                    put("content", msg.content)
                                })
                            }
                        })
                    }
                }
            }
            
            val request = buildRequest(config, jsonBody.toString())
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext ApiResult(
                    success = false,
                    error = "API错误: ${response.code} - ${response.body?.string()}"
                )
            }
            
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            
            val content = when {
                config.baseUrl.contains("anthropic") -> {
                    // Claude响应格式
                    jsonResponse.getJSONArray("content")
                        .getJSONObject(0)
                        .getString("text")
                }
                else -> {
                    // OpenAI响应格式
                    jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                }
            }
            
            ApiResult(success = true, data = content)
            
        } catch (e: Exception) {
            e.printStackTrace()
            ApiResult(success = false, error = e.message)
        }
    }
    
    /**
     * 图片反推 - 根据图片生成提示词
     */
    suspend fun imageToText(
        config: ApiConfig,
        imagePath: String,
        prompt: String = "请详细描述这张图片的内容，包括场景、物体、风格、光线等细节，生成一个适合AI绘画的英文提示词。"
    ): ApiResult<String> = withContext(Dispatchers.IO) {
        try {
            val base64Image = encodeImageToBase64(imagePath)
            
            val jsonBody = when {
                config.model.contains("vision") || config.baseUrl.contains("openai") -> {
                    // OpenAI Vision API
                    JSONObject().apply {
                        put("model", config.model)
                        put("max_tokens", 1000)
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("type", "text")
                                        put("text", prompt)
                                    })
                                    put(JSONObject().apply {
                                        put("type", "image_url")
                                        put("image_url", JSONObject().apply {
                                            put("url", "data:image/jpeg;base64,$base64Image")
                                        })
                                    })
                                })
                            })
                        })
                    }
                }
                else -> {
                    // Replicate等格式
                    JSONObject().apply {
                        put("version", config.model)
                        put("input", JSONObject().apply {
                            put("image", "data:image/jpeg;base64,$base64Image")
                        })
                    }
                }
            }
            
            val request = buildRequest(config, jsonBody.toString())
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext ApiResult(
                    success = false,
                    error = "API错误: ${response.code}"
                )
            }
            
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            
            // 解析响应
            val result = when {
                jsonResponse.has("choices") -> {
                    jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                }
                jsonResponse.has("output") -> {
                    jsonResponse.getString("output")
                }
                else -> responseBody
            }
            
            ApiResult(success = true, data = result)
            
        } catch (e: Exception) {
            e.printStackTrace()
            ApiResult(success = false, error = e.message)
        }
    }
    
    /**
     * 文生图API调用
     */
    suspend fun textToImage(
        config: ApiConfig,
        prompt: String,
        negativePrompt: String = "",
        width: Int = 1024,
        height: Int = 1024,
        steps: Int = 30
    ): ApiResult<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = when {
                config.baseUrl.contains("openai.com") && config.baseUrl.contains("images") -> {
                    // DALL-E API
                    JSONObject().apply {
                        put("model", config.model)
                        put("prompt", prompt)
                        put("n", 1)
                        put("size", "${width}x${height}")
                        put("response_format", "url")
                    }
                }
                config.baseUrl.contains("stability") -> {
                    // Stability AI API
                    JSONObject().apply {
                        put("text_prompts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                                put("weight", 1.0)
                            })
                            if (negativePrompt.isNotEmpty()) {
                                put(JSONObject().apply {
                                    put("text", negativePrompt)
                                    put("weight", -1.0)
                                })
                            }
                        })
                        put("cfg_scale", 7)
                        put("steps", steps)
                        put("width", width)
                        put("height", height)
                    }
                }
                config.baseUrl.contains("replicate") -> {
                    // Replicate API
                    JSONObject().apply {
                        put("version", config.model)
                        put("input", JSONObject().apply {
                            put("prompt", prompt)
                            put("negative_prompt", negativePrompt)
                            put("width", width)
                            put("height", height)
                            put("num_inference_steps", steps)
                        })
                    }
                }
                else -> {
                    // 通用格式
                    JSONObject().apply {
                        put("prompt", prompt)
                        put("negative_prompt", negativePrompt)
                        put("width", width)
                        put("height", height)
                        put("steps", steps)
                    }
                }
            }
            
            val request = buildRequest(config, jsonBody.toString())
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext ApiResult(
                    success = false,
                    error = "API错误: ${response.code} - ${response.body?.string()}"
                )
            }
            
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            
            // 解析图片URL或base64
            val result = when {
                jsonResponse.has("data") && jsonResponse.getJSONArray("data").length() > 0 -> {
                    // DALL-E格式
                    val data = jsonResponse.getJSONArray("data").getJSONObject(0)
                    if (data.has("url")) {
                        ImageGenerationResult(imageUrl = data.getString("url"))
                    } else {
                        ImageGenerationResult(imageBase64 = data.getString("b64_json"))
                    }
                }
                jsonResponse.has("artifacts") -> {
                    // Stability AI格式
                    val base64 = jsonResponse.getJSONArray("artifacts")
                        .getJSONObject(0)
                        .getString("base64")
                    ImageGenerationResult(imageBase64 = base64)
                }
                jsonResponse.has("urls") -> {
                    // 其他格式
                    ImageGenerationResult(imageUrl = jsonResponse.getJSONArray("urls").getString(0))
                }
                else -> {
                    ImageGenerationResult(imageUrl = responseBody)
                }
            }
            
            ApiResult(success = true, data = result)
            
        } catch (e: Exception) {
            e.printStackTrace()
            ApiResult(success = false, error = e.message)
        }
    }
    
    /**
     * 图生图API调用（上传参考图生成）
     */
    suspend fun imageToImage(
        config: ApiConfig,
        referenceImagePath: String,
        prompt: String,
        strength: Double = 0.75,
        steps: Int = 30
    ): ApiResult<ImageGenerationResult> = withContext(Dispatchers.IO) {
        try {
            val base64Image = encodeImageToBase64(referenceImagePath)
            
            val jsonBody = when {
                config.baseUrl.contains("stability") -> {
                    // Stability AI
                    JSONObject().apply {
                        put("text_prompts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                                put("weight", 1.0)
                            })
                        })
                        put("init_image", base64Image)
                        put("image_strength", strength)
                        put("cfg_scale", 7)
                        put("steps", steps)
                    }
                }
                config.baseUrl.contains("replicate") -> {
                    // Replicate
                    JSONObject().apply {
                        put("version", config.model)
                        put("input", JSONObject().apply {
                            put("image", "data:image/jpeg;base64,$base64Image")
                            put("prompt", prompt)
                            put("strength", strength)
                            put("num_inference_steps", steps)
                        })
                    }
                }
                else -> {
                    // 通用格式
                    JSONObject().apply {
                        put("prompt", prompt)
                        put("init_image", base64Image)
                        put("strength", strength)
                        put("steps", steps)
                    }
                }
            }
            
            val request = buildRequest(config, jsonBody.toString())
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext ApiResult(
                    success = false,
                    error = "API错误: ${response.code}"
                )
            }
            
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            
            val result = when {
                jsonResponse.has("artifacts") -> {
                    val base64 = jsonResponse.getJSONArray("artifacts")
                        .getJSONObject(0)
                        .getString("base64")
                    ImageGenerationResult(imageBase64 = base64)
                }
                jsonResponse.has("output") -> {
                    val output = jsonResponse.get("output")
                    if (output is JSONArray) {
                        ImageGenerationResult(imageUrl = output.getString(0))
                    } else {
                        ImageGenerationResult(imageUrl = output.toString())
                    }
                }
                else -> ImageGenerationResult(imageUrl = responseBody)
            }
            
            ApiResult(success = true, data = result)
            
        } catch (e: Exception) {
            e.printStackTrace()
            ApiResult(success = false, error = e.message)
        }
    }
    
    /**
     * 视频生成API调用
     */
    suspend fun generateVideo(
        config: ApiConfig,
        prompt: String? = null,
        imagePath: String? = null,
        duration: Int = 4
    ): ApiResult<VideoGenerationResult> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = when {
                config.baseUrl.contains("runway") -> {
                    // Runway Gen-2
                    JSONObject().apply {
                        put("prompt", prompt ?: "")
                        if (imagePath != null) {
                            put("image_prompt", encodeImageToBase64(imagePath))
                        }
                        put("duration", duration)
                    }
                }
                config.baseUrl.contains("replicate") -> {
                    // Replicate视频模型
                    JSONObject().apply {
                        put("version", config.model)
                        put("input", JSONObject().apply {
                            if (imagePath != null) {
                                put("image", encodeImageToBase64(imagePath))
                            }
                            if (prompt != null) {
                                put("prompt", prompt)
                            }
                            put("fps", 24)
                            put("num_frames", duration * 24)
                        })
                    }
                }
                else -> {
                    // 通用格式
                    JSONObject().apply {
                        put("prompt", prompt ?: "")
                        if (imagePath != null) {
                            put("image", encodeImageToBase64(imagePath))
                        }
                        put("duration", duration)
                    }
                }
            }
            
            val request = buildRequest(config, jsonBody.toString())
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext ApiResult(
                    success = false,
                    error = "API错误: ${response.code}"
                )
            }
            
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            
            // 解析响应（视频API通常返回任务ID或URL）
            val result = when {
                jsonResponse.has("id") -> {
                    // 异步任务
                    VideoGenerationResult(
                        status = "pending",
                        videoUrl = jsonResponse.optString("urls", null)
                            ?.let { JSONObject(it).optString("get") }
                    )
                }
                jsonResponse.has("output") -> {
                    val output = jsonResponse.get("output")
                    if (output is String) {
                        VideoGenerationResult(videoUrl = output, status = "completed")
                    } else if (output is JSONArray && output.length() > 0) {
                        VideoGenerationResult(videoUrl = output.getString(0), status = "completed")
                    } else {
                        VideoGenerationResult(status = "pending")
                    }
                }
                else -> VideoGenerationResult(status = "pending")
            }
            
            ApiResult(success = true, data = result)
            
        } catch (e: Exception) {
            e.printStackTrace()
            ApiResult(success = false, error = e.message)
        }
    }
    
    /**
     * 检查视频生成状态（用于异步API）
     */
    suspend fun checkVideoStatus(
        config: ApiConfig,
        predictionUrl: String
    ): ApiResult<VideoGenerationResult> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(predictionUrl)
                .header("Authorization", "Token ${config.apiKey}")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext ApiResult(
                    success = false,
                    error = "API错误: ${response.code}"
                )
            }
            
            val responseBody = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBody)
            
            val status = jsonResponse.optString("status", "unknown")
            val output = jsonResponse.opt("output")
            
            val result = VideoGenerationResult(
                status = status,
                videoUrl = when (output) {
                    is String -> output
                    is JSONArray -> if (output.length() > 0) output.getString(0) else null
                    else -> null
                }
            )
            
            ApiResult(success = true, data = result)
            
        } catch (e: Exception) {
            e.printStackTrace()
            ApiResult(success = false, error = e.message)
        }
    }
    
    /**
     * 构建HTTP请求
     */
    private fun buildRequest(config: ApiConfig, body: String): Request {
        val requestBuilder = Request.Builder()
            .url(config.baseUrl)
            .post(body.toRequestBody("application/json".toMediaType()))
        
        // 添加认证头
        requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
        
        // 添加额外请求头
        if (config.headers.isNotEmpty()) {
            try {
                val headersJson = JSONObject(config.headers)
                headersJson.keys().forEach { key ->
                    requestBuilder.header(key, headersJson.getString(key))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return requestBuilder.build()
    }
    
    /**
     * 将图片编码为Base64
     */
    private fun encodeImageToBase64(imagePath: String): String {
        val file = File(imagePath)
        if (!file.exists()) throw IOException("图片文件不存在: $imagePath")
        
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    /**
     * 下载图片并保存到本地
     */
    suspend fun downloadImage(imageUrl: String, saveDir: File): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(imageUrl).build()
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("下载失败: ${response.code}")
        }
        
        val fileName = "AI_GEN_${System.currentTimeMillis()}.jpg"
        val outputFile = File(saveDir, fileName)
        
        response.body?.byteStream()?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        outputFile.absolutePath
    }
    
    /**
     * 下载视频并保存到本地
     */
    suspend fun downloadVideo(videoUrl: String, saveDir: File): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(videoUrl).build()
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw IOException("下载失败: ${response.code}")
        }
        
        val fileName = "AI_VIDEO_${System.currentTimeMillis()}.mp4"
        val outputFile = File(saveDir, fileName)
        
        response.body?.byteStream()?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        outputFile.absolutePath
    }
}
