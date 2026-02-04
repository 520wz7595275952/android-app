package com.promptimagemanager.ui

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.promptimagemanager.R
import com.promptimagemanager.data.Category
import com.promptimagemanager.data.ImageItem
import com.promptimagemanager.data.Repository
import com.promptimagemanager.databinding.ActivityAddImageBinding
import com.promptimagemanager.utils.ImageStorageManager
import kotlinx.coroutines.launch

class AddImageActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
    
    private lateinit var binding: ActivityAddImageBinding
    private lateinit var repository: Repository
    private lateinit var imageStorageManager: ImageStorageManager
    
    private var imageUri: Uri? = null
    private var categories: List<Category> = emptyList()
    private var selectedCategoryId: Long? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        repository = Repository(this)
        imageStorageManager = ImageStorageManager(this)
        
        setupToolbar()
        loadCategories()
        
        // Get image URI from intent
        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString != null) {
            imageUri = Uri.parse(uriString)
            displayImage()
        }
        
        binding.btnSave.setOnClickListener {
            saveImage()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_launcher_foreground)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun displayImage() {
        imageUri?.let { uri ->
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(binding.ivPreview)
        }
    }
    
    private fun loadCategories() {
        lifecycleScope.launch {
            val cats = repository.getAllCategoriesSync()
            categories = cats
            
            val categoryNames = listOf("未分类") + cats.map { it.name }
            val adapter = ArrayAdapter(
                this@AddImageActivity,
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
            )
            binding.spinnerCategory.setAdapter(adapter)
            
            binding.spinnerCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = if (position == 0) null else cats.getOrNull(position - 1)?.id
            }
        }
    }
    
    private fun saveImage() {
        val uri = imageUri
        if (uri == null) {
            Toast.makeText(this, "请先选择图片", Toast.LENGTH_SHORT).show()
            return
        }
        
        val prompt = binding.etPrompt.text.toString().trim()
        
        lifecycleScope.launch {
            try {
                // Save image to private storage (not visible in gallery)
                val savedPath = imageStorageManager.saveImageToPrivateStorage(uri)
                
                // Create image item
                val imageItem = ImageItem(
                    imagePath = savedPath,
                    prompt = prompt,
                    categoryId = selectedCategoryId
                )
                
                // Save to database
                repository.insertImage(imageItem)
                
                Toast.makeText(this@AddImageActivity, "图片已保存", Toast.LENGTH_SHORT).show()
                finish()
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@AddImageActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
