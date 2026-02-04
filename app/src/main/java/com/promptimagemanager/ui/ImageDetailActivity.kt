package com.promptimagemanager.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.promptimagemanager.R
import com.promptimagemanager.api.ApiService
import com.promptimagemanager.data.ApiConfig
import com.promptimagemanager.data.Category
import com.promptimagemanager.data.ImageItem
import com.promptimagemanager.data.Repository
import com.promptimagemanager.databinding.ActivityImageDetailBinding
import com.promptimagemanager.utils.ClipboardHelper
import com.promptimagemanager.utils.ImageStorageManager
import kotlinx.coroutines.launch

class ImageDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_ID = "extra_image_id"
    }
    
    private lateinit var binding: ActivityImageDetailBinding
    private lateinit var repository: Repository
    private lateinit var imageStorageManager: ImageStorageManager
    private lateinit var apiService: ApiService
    
    private var imageId: Long = -1
    private var currentImage: ImageItem? = null
    private var categories: List<Category> = emptyList()
    private var selectedCategoryId: Long? = null
    
    private lateinit var btnReversePrompt: MaterialButton
    private lateinit var btnGenerateSimilar: MaterialButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        repository = Repository(this)
        imageStorageManager = ImageStorageManager(this)
        apiService = ApiService(this)
        
        setupToolbar()
        initExtraButtons()
        
        imageId = intent.getLongExtra(EXTRA_IMAGE_ID, -1)
        if (imageId == -1L) {
            Toast.makeText(this, "图片不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        loadCategories()
        loadImage()
        
        binding.btnCopy.setOnClickListener {
            val prompt = binding.etPrompt.text.toString()
            ClipboardHelper.copyText(this, prompt)
        }
        
        binding.btnSave.setOnClickListener {
            updateImage()
        }
        
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmDialog()
        }
    }
    
    private fun initExtraButtons() {
        btnReversePrompt = findViewById(R.id.btnReversePrompt)
        btnGenerateSimilar = findViewById(R.id.btnGenerateSimilar)
        
        btnReversePrompt.setOnClickListener {
            reversePrompt()
        }
        
        btnGenerateSimilar.setOnClickListener {
            generateSimilarImage()
        }
    }
    
    /**
     * 图片反推 - 使用AI分析图片生成提示词
     */
    private fun reversePrompt() {
        val image = currentImage ?: return
        
        lifecycleScope.launch {
            // 获取图片反推API配置
            val configs = repository.getApiConfigsByTypeSync(ApiConfig.TYPE_IMAGE_TO_TEXT)
            if (configs.isEmpty()) {
                Toast.makeText(
                    this@ImageDetailActivity,
                    "请先配置图片反推API",
                    Toast.LENGTH_SHORT
                ).show()
                // 跳转到API配置页面
                startActivity(Intent(this@ImageDetailActivity, ApiConfigActivity::class.java))
                return@launch
            }
            
            val config = configs.find { it.isDefault } ?: configs.first()
            
            btnReversePrompt.isEnabled = false
            btnReversePrompt.text = "分析中..."
            
            try {
                val result = apiService.imageToText(config, image.imagePath)
                
                btnReversePrompt.isEnabled = true
                btnReversePrompt.text = "图片反推"
                
                if (result.success) {
                    val generatedPrompt = result.data ?: ""
                    // 显示对话框让用户选择是否使用生成的提示词
                    AlertDialog.Builder(this@ImageDetailActivity)
                        .setTitle("图片反推结果")
                        .setMessage(generatedPrompt)
                        .setPositiveButton("使用") { _, _ ->
                            binding.etPrompt.setText(generatedPrompt)
                        }
                        .setNeutralButton("复制") { _, _ ->
                            ClipboardHelper.copyText(this@ImageDetailActivity, generatedPrompt)
                        }
                        .setNegativeButton("取消", null)
                        .show()
                } else {
                    Toast.makeText(
                        this@ImageDetailActivity,
                        "反推失败: ${result.error}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                btnReversePrompt.isEnabled = true
                btnReversePrompt.text = "图片反推"
                Toast.makeText(
                    this@ImageDetailActivity,
                    "错误: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * 生成相似图片
     */
    private fun generateSimilarImage() {
        val image = currentImage ?: return
        
        lifecycleScope.launch {
            // 获取图片生成API配置
            val configs = repository.getApiConfigsByTypeSync(ApiConfig.TYPE_TEXT_TO_IMAGE)
            if (configs.isEmpty()) {
                Toast.makeText(
                    this@ImageDetailActivity,
                    "请先配置图片生成API",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@ImageDetailActivity, ApiConfigActivity::class.java))
                return@launch
            }
            
            val config = configs.find { it.isDefault } ?: configs.first()
            
            // 跳转到图片生成页面，传入参考图
            val intent = Intent(this@ImageDetailActivity, ImageGenerationActivity::class.java).apply {
                putExtra(ImageGenerationActivity.EXTRA_API_CONFIG_ID, config.id)
                putExtra(ImageGenerationActivity.EXTRA_REFERENCE_IMAGE, image.imagePath)
                // 如果有提示词，也传过去
                if (image.prompt.isNotEmpty()) {
                    putExtra("prompt", image.prompt)
                }
            }
            startActivity(intent)
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun loadCategories() {
        lifecycleScope.launch {
            categories = repository.getAllCategoriesSync()
        }
    }
    
    private fun loadImage() {
        lifecycleScope.launch {
            val image = repository.getImageById(imageId)
            if (image == null) {
                Toast.makeText(this@ImageDetailActivity, "图片不存在", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            
            currentImage = image
            displayImage(image)
        }
    }
    
    private fun displayImage(image: ImageItem) {
        Glide.with(this)
            .load(image.imagePath)
            .fitCenter()
            .into(binding.ivImage)
        
        binding.etPrompt.setText(image.prompt)
        
        // Setup category spinner
        val categoryNames = listOf("未分类") + categories.map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categoryNames
        )
        // Category selection can be added here if needed
    }
    
    private fun updateImage() {
        val image = currentImage ?: return
        
        val newPrompt = binding.etPrompt.text.toString().trim()
        
        lifecycleScope.launch {
            val updatedImage = image.copy(
                prompt = newPrompt,
                categoryId = selectedCategoryId
            )
            
            repository.updateImage(updatedImage)
            Toast.makeText(this@ImageDetailActivity, "已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("删除图片")
            .setMessage("确定要删除这张图片吗？此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                deleteImage()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun deleteImage() {
        val image = currentImage ?: return
        
        lifecycleScope.launch {
            // Delete image file
            imageStorageManager.deleteImage(image.imagePath)
            
            // Delete from database
            repository.deleteImage(image)
            
            Toast.makeText(this@ImageDetailActivity, "已删除", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
