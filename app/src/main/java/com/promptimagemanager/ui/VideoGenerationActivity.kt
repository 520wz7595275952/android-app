package com.promptimagemanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.promptimagemanager.R
import com.promptimagemanager.api.ApiService
import com.promptimagemanager.data.ApiConfig
import com.promptimagemanager.data.Repository
import com.promptimagemanager.utils.ImageStorageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * 视频生成界面
 */
class VideoGenerationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_API_CONFIG_ID = "extra_api_config_id"
        const val EXTRA_REFERENCE_IMAGE = "extra_reference_image"
    }
    
    private var apiConfigId: Long = -1
    private var referenceImagePath: String? = null
    private lateinit var apiConfig: ApiConfig
    private lateinit var repository: Repository
    private lateinit var apiService: ApiService
    private lateinit var imageStorageManager: ImageStorageManager
    
    private lateinit var etPrompt: EditText
    private lateinit var ivReference: ImageView
    private lateinit var cardReference: CardView
    private lateinit var sliderDuration: Slider
    private lateinit var btnSelectReference: MaterialButton
    private lateinit var btnGenerate: MaterialButton
    private lateinit var btnPlay: MaterialButton
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    
    private var generatedVideoPath: String? = null
    private var predictionUrl: String? = null
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                try {
                    referenceImagePath = imageStorageManager.saveImageToPrivateStorage(it)
                    displayReferenceImage()
                } catch (e: Exception) {
                    Toast.makeText(this@VideoGenerationActivity, "加载图片失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_generation)
        
        apiConfigId = intent.getLongExtra(EXTRA_API_CONFIG_ID, -1)
        referenceImagePath = intent.getStringExtra(EXTRA_REFERENCE_IMAGE)
        
        if (apiConfigId == -1L) {
            Toast.makeText(this, "未选择API配置", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        repository = Repository(this)
        apiService = ApiService(this)
        imageStorageManager = ImageStorageManager(this)
        
        initViews()
        setupToolbar()
        loadApiConfig()
        
        referenceImagePath?.let {
            displayReferenceImage()
        }
    }
    
    private fun initViews() {
        etPrompt = findViewById(R.id.etPrompt)
        ivReference = findViewById(R.id.ivReference)
        cardReference = findViewById(R.id.cardReference)
        sliderDuration = findViewById(R.id.sliderDuration)
        btnSelectReference = findViewById(R.id.btnSelectReference)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnPlay = findViewById(R.id.btnPlay)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        
        btnSelectReference.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        
        btnGenerate.setOnClickListener {
            generateVideo()
        }
        
        btnPlay.setOnClickListener {
            playVideo()
        }
        
        // 设置滑块默认值
        sliderDuration.value = 4f
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "AI视频生成"
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun displayReferenceImage() {
        referenceImagePath?.let { path ->
            cardReference.visibility = View.VISIBLE
            Glide.with(this)
                .load(path)
                .centerCrop()
                .into(ivReference)
        } ?: run {
            cardReference.visibility = View.GONE
        }
    }
    
    private fun loadApiConfig() {
        lifecycleScope.launch {
            val config = repository.getApiConfigById(apiConfigId)
            if (config == null) {
                Toast.makeText(this@VideoGenerationActivity, "API配置不存在", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            apiConfig = config
        }
    }
    
    private fun generateVideo() {
        val prompt = etPrompt.text.toString().trim()
        val duration = sliderDuration.value.toInt()
        
        if (prompt.isEmpty() && referenceImagePath == null) {
            Toast.makeText(this, "请输入提示词或选择参考图片", Toast.LENGTH_SHORT).show()
            return
        }
        
        progressBar.visibility = View.VISIBLE
        btnGenerate.isEnabled = false
        tvStatus.text = "正在生成视频..."
        tvStatus.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val result = apiService.generateVideo(
                    config = apiConfig,
                    prompt = prompt.ifEmpty { null },
                    imagePath = referenceImagePath,
                    duration = duration
                )
                
                if (result.success) {
                    val videoResult = result.data
                    if (videoResult != null) {
                        if (videoResult.status == "completed" && videoResult.videoUrl != null) {
                            // 直接完成
                            downloadVideo(videoResult.videoUrl)
                        } else if (videoResult.videoUrl != null) {
                            // 异步任务，需要轮询
                            predictionUrl = videoResult.videoUrl
                            pollVideoStatus()
                        }
                    }
                } else {
                    progressBar.visibility = View.GONE
                    btnGenerate.isEnabled = true
                    tvStatus.text = "生成失败: ${result.error}"
                    Toast.makeText(
                        this@VideoGenerationActivity,
                        "生成失败: ${result.error}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                btnGenerate.isEnabled = true
                tvStatus.text = "错误: ${e.message}"
                Toast.makeText(
                    this@VideoGenerationActivity,
                    "错误: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private suspend fun pollVideoStatus() {
        val url = predictionUrl ?: return
        
        var attempts = 0
        val maxAttempts = 60  // 最多等待5分钟
        
        while (attempts < maxAttempts) {
            delay(5000)  // 每5秒检查一次
            
            val result = apiService.checkVideoStatus(apiConfig, url)
            
            if (result.success) {
                val status = result.data
                if (status != null) {
                    when (status.status) {
                        "succeeded", "completed" -> {
                            if (status.videoUrl != null) {
                                downloadVideo(status.videoUrl)
                                return
                            }
                        }
                        "failed" -> {
                            progressBar.visibility = View.GONE
                            btnGenerate.isEnabled = true
                            tvStatus.text = "生成失败"
                            return
                        }
                        else -> {
                            tvStatus.text = "生成中... (${attempts + 1}/$maxAttempts)"
                        }
                    }
                }
            }
            
            attempts++
        }
        
        // 超时
        progressBar.visibility = View.GONE
        btnGenerate.isEnabled = true
        tvStatus.text = "生成超时，请稍后检查"
    }
    
    private suspend fun downloadVideo(videoUrl: String) {
        try {
            val videosDir = File(filesDir, "GeneratedVideos")
            if (!videosDir.exists()) videosDir.mkdirs()
            
            val savedPath = apiService.downloadVideo(videoUrl, videosDir)
            generatedVideoPath = savedPath
            
            progressBar.visibility = View.GONE
            btnGenerate.isEnabled = true
            tvStatus.text = "视频生成完成"
            btnPlay.visibility = View.VISIBLE
            
            Toast.makeText(this@VideoGenerationActivity, "视频已保存", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            progressBar.visibility = View.GONE
            btnGenerate.isEnabled = true
            tvStatus.text = "下载失败: ${e.message}"
            Toast.makeText(this@VideoGenerationActivity, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun playVideo() {
        val path = generatedVideoPath ?: return
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            File(path)
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(intent)
    }
}
