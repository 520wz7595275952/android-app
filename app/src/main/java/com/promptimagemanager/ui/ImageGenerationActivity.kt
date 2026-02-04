package com.promptimagemanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.promptimagemanager.data.ImageItem
import com.promptimagemanager.data.Repository
import com.promptimagemanager.utils.ImageStorageManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * 图片生成界面（文生图/图生图）
 */
class ImageGenerationActivity : AppCompatActivity() {

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
    private lateinit var etNegativePrompt: EditText
    private lateinit var ivReference: ImageView
    private lateinit var cardReference: CardView
    private lateinit var sliderSteps: Slider
    private lateinit var sliderStrength: Slider
    private lateinit var btnSelectReference: MaterialButton
    private lateinit var btnGenerate: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var ivResult: ImageView
    private lateinit var progressBar: ProgressBar
    
    private var generatedImagePath: String? = null
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                try {
                    referenceImagePath = imageStorageManager.saveImageToPrivateStorage(it)
                    displayReferenceImage()
                } catch (e: Exception) {
                    Toast.makeText(this@ImageGenerationActivity, "加载图片失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_generation)
        
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
        etNegativePrompt = findViewById(R.id.etNegativePrompt)
        ivReference = findViewById(R.id.ivReference)
        cardReference = findViewById(R.id.cardReference)
        sliderSteps = findViewById(R.id.sliderSteps)
        sliderStrength = findViewById(R.id.sliderStrength)
        btnSelectReference = findViewById(R.id.btnSelectReference)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnSave = findViewById(R.id.btnSave)
        ivResult = findViewById(R.id.ivResult)
        progressBar = findViewById(R.id.progressBar)
        
        btnSelectReference.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        
        btnGenerate.setOnClickListener {
            generateImage()
        }
        
        btnSave.setOnClickListener {
            saveGeneratedImage()
        }
        
        // 设置滑块默认值
        sliderSteps.value = 30f
        sliderStrength.value = 0.75f
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "AI图片生成"
        
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
                Toast.makeText(this@ImageGenerationActivity, "API配置不存在", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            apiConfig = config
        }
    }
    
    private fun generateImage() {
        val prompt = etPrompt.text.toString().trim()
        if (prompt.isEmpty()) {
            Toast.makeText(this, "请输入提示词", Toast.LENGTH_SHORT).show()
            return
        }
        
        val negativePrompt = etNegativePrompt.text.toString().trim()
        val steps = sliderSteps.value.toInt()
        val strength = sliderStrength.value.toDouble()
        
        progressBar.visibility = View.VISIBLE
        btnGenerate.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val result = if (referenceImagePath != null) {
                    // 图生图
                    apiService.imageToImage(
                        config = apiConfig,
                        referenceImagePath = referenceImagePath!!,
                        prompt = prompt,
                        strength = strength,
                        steps = steps
                    )
                } else {
                    // 文生图
                    apiService.textToImage(
                        config = apiConfig,
                        prompt = prompt,
                        negativePrompt = negativePrompt,
                        steps = steps
                    )
                }
                
                progressBar.visibility = View.GONE
                btnGenerate.isEnabled = true
                
                if (result.success) {
                    val imageResult = result.data
                    if (imageResult != null) {
                        displayGeneratedImage(imageResult)
                    }
                } else {
                    Toast.makeText(
                        this@ImageGenerationActivity,
                        "生成失败: ${result.error}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                btnGenerate.isEnabled = true
                Toast.makeText(
                    this@ImageGenerationActivity,
                    "错误: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private suspend fun displayGeneratedImage(result: ApiService.Companion.ImageGenerationResult) {
        when {
            result.imageUrl != null -> {
                // 下载图片
                val imagesDir = File(filesDir, "GeneratedImages")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                
                try {
                    val savedPath = apiService.downloadImage(result.imageUrl, imagesDir)
                    generatedImagePath = savedPath
                    
                    Glide.with(this@ImageGenerationActivity)
                        .load(savedPath)
                        .fitCenter()
                        .into(ivResult)
                    
                    btnSave.visibility = View.VISIBLE
                    
                } catch (e: Exception) {
                    Toast.makeText(this@ImageGenerationActivity, "下载图片失败", Toast.LENGTH_SHORT).show()
                }
            }
            result.imageBase64 != null -> {
                // 解码base64
                val decodedBytes = android.util.Base64.decode(result.imageBase64, android.util.Base64.DEFAULT)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                
                // 保存到本地
                val imagesDir = File(filesDir, "GeneratedImages")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                
                val fileName = "AI_GEN_${System.currentTimeMillis()}.jpg"
                val outputFile = File(imagesDir, fileName)
                
                outputFile.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                }
                
                generatedImagePath = outputFile.absolutePath
                ivResult.setImageBitmap(bitmap)
                btnSave.visibility = View.VISIBLE
            }
        }
    }
    
    private fun saveGeneratedImage() {
        val path = generatedImagePath
        if (path == null) {
            Toast.makeText(this, "没有可保存的图片", Toast.LENGTH_SHORT).show()
            return
        }
        
        val prompt = etPrompt.text.toString().trim()
        
        lifecycleScope.launch {
            try {
                // 移动到应用图片目录
                val imagesDir = File(filesDir, "PromptImages")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                
                val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                val destFile = File(imagesDir, fileName)
                
                File(path).copyTo(destFile, overwrite = true)
                File(path).delete()
                
                // 保存到数据库
                val imageItem = ImageItem(
                    imagePath = destFile.absolutePath,
                    prompt = prompt
                )
                repository.insertImage(imageItem)
                
                Toast.makeText(this@ImageGenerationActivity, "图片已保存", Toast.LENGTH_SHORT).show()
                finish()
                
            } catch (e: Exception) {
                Toast.makeText(this@ImageGenerationActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
