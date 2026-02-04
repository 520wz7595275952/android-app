package com.promptimagemanager.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.promptimagemanager.R
import com.promptimagemanager.adapter.ImageAdapter
import com.promptimagemanager.data.ApiConfig
import com.promptimagemanager.data.Category
import com.promptimagemanager.data.ImageItem
import com.promptimagemanager.data.Repository
import com.promptimagemanager.databinding.ActivityMainBinding
import com.promptimagemanager.utils.ClipboardHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: Repository
    private lateinit var imageAdapter: ImageAdapter
    
    private var categories: List<Category> = emptyList()
    private var currentCategoryId: Long? = null
    private var currentQuery: String = ""
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val intent = Intent(this, AddImageActivity::class.java).apply {
                putExtra(AddImageActivity.EXTRA_IMAGE_URI, it.toString())
            }
            startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        repository = Repository(this)
        
        setupToolbar()
        setupRecyclerView()
        setupTabLayout()
        setupSearchView()
        setupFab()
        
        loadCategories()
        loadImages()
    }
    
    override fun onResume() {
        super.onResume()
        loadImages()
        loadCategories()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }
    
    private fun setupRecyclerView() {
        imageAdapter = ImageAdapter(
            onItemClick = { image ->
                val intent = Intent(this, ImageDetailActivity::class.java).apply {
                    putExtra(ImageDetailActivity.EXTRA_IMAGE_ID, image.id)
                }
                startActivity(intent)
            },
            onCopyClick = { image ->
                ClipboardHelper.copyText(this, image.prompt)
            },
            onEditClick = { image ->
                val intent = Intent(this, ImageDetailActivity::class.java).apply {
                    putExtra(ImageDetailActivity.EXTRA_IMAGE_ID, image.id)
                }
                startActivity(intent)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = imageAdapter
        }
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        currentCategoryId = null
                        loadImages()
                    }
                    else -> {
                        val category = categories.getOrNull(tab?.position?.minus(1) ?: -1)
                        currentCategoryId = category?.id
                        loadImages()
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                if (currentQuery.isNotEmpty()) {
                    searchImages(currentQuery)
                } else {
                    loadImages()
                }
                return true
            }
        })
    }
    
    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddImageDialog()
        }
    }
    
    private fun showAddImageDialog() {
        val options = arrayOf("从相册选择", "管理分类")
        AlertDialog.Builder(this)
            .setTitle("添加图片")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> startActivity(Intent(this, CategoryActivity::class.java))
                }
            }
            .show()
    }
    
    private fun loadCategories() {
        lifecycleScope.launch {
            repository.getAllCategories().observe(this@MainActivity) { cats ->
                categories = cats
                updateTabs()
                updateCategoryMap()
            }
        }
    }
    
    private fun updateTabs() {
        binding.tabLayout.removeAllTabs()
        
        // Add "All" tab
        binding.tabLayout.addTab(
            binding.tabLayout.newTab().setText("全部")
        )
        
        // Add category tabs
        categories.forEach { category ->
            binding.tabLayout.addTab(
                binding.tabLayout.newTab().setText(category.name)
            )
        }
    }
    
    private fun updateCategoryMap() {
        val categoryMap = categories.associateBy({ it.id }, { it.name })
        imageAdapter.setCategoryMap(categoryMap)
    }
    
    private fun loadImages() {
        val liveData = if (currentCategoryId != null) {
            repository.getImagesByCategory(currentCategoryId!!)
        } else {
            repository.getAllImages()
        }
        
        liveData.observe(this) { images ->
            imageAdapter.submitList(images)
            binding.tvEmpty.visibility = if (images.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    private fun searchImages(query: String) {
        repository.searchImages(query).observe(this) { images ->
            imageAdapter.submitList(images)
            binding.tvEmpty.visibility = if (images.isEmpty()) View.VISIBLE else View.GONE
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_categories -> {
                startActivity(Intent(this, CategoryActivity::class.java))
                true
            }
            R.id.action_api_config -> {
                startActivity(Intent(this, ApiConfigActivity::class.java))
                true
            }
            R.id.action_ai_chat -> {
                openChatActivity()
                true
            }
            R.id.action_image_generation -> {
                openImageGenerationActivity()
                true
            }
            R.id.action_video_generation -> {
                openVideoGenerationActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun openChatActivity() {
        lifecycleScope.launch {
            val configs = repository.getApiConfigsByTypeSync(ApiConfig.TYPE_CHAT)
            if (configs.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "请先配置聊天API",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@MainActivity, ApiConfigActivity::class.java))
                return@launch
            }
            
            val config = configs.find { it.isDefault } ?: configs.first()
            val intent = Intent(this@MainActivity, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_API_CONFIG_ID, config.id)
            }
            startActivity(intent)
        }
    }
    
    private fun openImageGenerationActivity() {
        lifecycleScope.launch {
            val configs = repository.getApiConfigsByTypeSync(ApiConfig.TYPE_TEXT_TO_IMAGE)
            if (configs.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "请先配置图片生成API",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@MainActivity, ApiConfigActivity::class.java))
                return@launch
            }
            
            val config = configs.find { it.isDefault } ?: configs.first()
            val intent = Intent(this@MainActivity, ImageGenerationActivity::class.java).apply {
                putExtra(ImageGenerationActivity.EXTRA_API_CONFIG_ID, config.id)
            }
            startActivity(intent)
        }
    }
    
    private fun openVideoGenerationActivity() {
        lifecycleScope.launch {
            val configs = repository.getApiConfigsByTypeSync(ApiConfig.TYPE_VIDEO)
            if (configs.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "请先配置视频生成API",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this@MainActivity, ApiConfigActivity::class.java))
                return@launch
            }
            
            val config = configs.find { it.isDefault } ?: configs.first()
            val intent = Intent(this@MainActivity, VideoGenerationActivity::class.java).apply {
                putExtra(VideoGenerationActivity.EXTRA_API_CONFIG_ID, config.id)
            }
            startActivity(intent)
        }
    }
}
