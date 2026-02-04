package com.promptimagemanager.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.promptimagemanager.R
import com.promptimagemanager.adapter.ShareImageAdapter
import com.promptimagemanager.data.Category
import com.promptimagemanager.data.ImageItem
import com.promptimagemanager.data.Repository
import com.promptimagemanager.databinding.ActivityShareReceiverBinding
import com.promptimagemanager.utils.ImageStorageManager
import kotlinx.coroutines.launch

class ShareReceiverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareReceiverBinding
    private lateinit var repository: Repository
    private lateinit var imageStorageManager: ImageStorageManager
    private lateinit var shareImageAdapter: ShareImageAdapter
    
    private var imageUris: List<Uri> = emptyList()
    private var categories: List<Category> = emptyList()
    private var selectedCategoryId: Long? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        repository = Repository(this)
        imageStorageManager = ImageStorageManager(this)
        
        setupToolbar()
        setupRecyclerView()
        loadCategories()
        
        // Handle shared images
        handleSharedImages()
        
        binding.btnSave.setOnClickListener {
            saveSharedImages()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        shareImageAdapter = ShareImageAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ShareReceiverActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = shareImageAdapter
        }
    }
    
    private fun loadCategories() {
        lifecycleScope.launch {
            categories = repository.getAllCategoriesSync()
            
            val categoryNames = listOf("未分类") + categories.map { it.name }
            val adapter = ArrayAdapter(
                this@ShareReceiverActivity,
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
            )
            binding.spinnerCategory.setAdapter(adapter)
            
            binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = if (position == 0) null else categories.getOrNull(position - 1)?.id
            }
        }
    }
    
    private fun handleSharedImages() {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                        imageUris = listOf(uri)
                        shareImageAdapter.setImages(imageUris)
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (intent.type?.startsWith("image/") == true) {
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris ->
                        imageUris = uris
                        shareImageAdapter.setImages(imageUris)
                    }
                }
            }
        }
        
        if (imageUris.isEmpty()) {
            Toast.makeText(this, "没有接收到图片", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun saveSharedImages() {
        if (imageUris.isEmpty()) {
            Toast.makeText(this, "没有图片需要保存", Toast.LENGTH_SHORT).show()
            return
        }
        
        val prompt = binding.etPrompt.text.toString().trim()
        
        lifecycleScope.launch {
            try {
                var savedCount = 0
                
                imageUris.forEach { uri ->
                    try {
                        // Save image to private storage
                        val savedPath = imageStorageManager.saveImageToPrivateStorage(uri)
                        
                        // Create image item
                        val imageItem = ImageItem(
                            imagePath = savedPath,
                            prompt = prompt,
                            categoryId = selectedCategoryId
                        )
                        
                        // Save to database
                        repository.insertImage(imageItem)
                        savedCount++
                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                Toast.makeText(
                    this@ShareReceiverActivity, 
                    "已保存 $savedCount 张图片", 
                    Toast.LENGTH_SHORT
                ).show()
                
                // Go to main activity
                val intent = Intent(this@ShareReceiverActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ShareReceiverActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
