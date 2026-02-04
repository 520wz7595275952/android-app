package com.promptimagemanager.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.promptimagemanager.R
import com.promptimagemanager.data.ApiConfig
import com.promptimagemanager.data.ApiPresets
import com.promptimagemanager.data.Repository
import kotlinx.coroutines.launch

/**
 * API配置管理界面
 */
class ApiConfigActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var repository: Repository
    private lateinit var adapter: ApiConfigAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_config)
        
        repository = Repository(this)
        
        setupToolbar()
        setupRecyclerView()
        setupFab()
        
        loadConfigs()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "API配置管理"
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        adapter = ApiConfigAdapter(
            onEditClick = { config ->
                editConfig(config)
            },
            onDeleteClick = { config ->
                showDeleteConfirmDialog(config)
            },
            onSetDefaultClick = { config ->
                setAsDefault(config)
            },
            onTestClick = { config ->
                testConfig(config)
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupFab() {
        fabAdd = findViewById(R.id.fabAdd)
        fabAdd.setOnClickListener {
            showAddOptions()
        }
    }
    
    private fun showAddOptions() {
        val options = arrayOf("手动添加配置", "从预设添加", "导入配置")
        AlertDialog.Builder(this)
            .setTitle("添加API配置")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> addConfigManually()
                    1 -> showPresetConfigs()
                    2 -> importConfig()
                }
            }
            .show()
    }
    
    private fun addConfigManually() {
        val intent = Intent(this, ApiConfigEditActivity::class.java)
        startActivity(intent)
    }
    
    private fun showPresetConfigs() {
        val presets = ApiPresets.getAllPresets("your-api-key")
        val presetNames = presets.map { "${it.name} (${ApiConfig.getTypeName(it.apiType)})" }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("选择预设")
            .setItems(presetNames) { _, which ->
                val preset = presets[which]
                showApiKeyDialog(preset)
            }
            .show()
    }
    
    private fun showApiKeyDialog(preset: ApiConfig) {
        val editText = android.widget.EditText(this).apply {
            hint = "输入API Key"
        }
        
        AlertDialog.Builder(this)
            .setTitle("配置 ${preset.name}")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val apiKey = editText.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    lifecycleScope.launch {
                        val config = preset.copy(apiKey = apiKey)
                        repository.insertApiConfig(config)
                        Toast.makeText(this@ApiConfigActivity, "配置已添加", Toast.LENGTH_SHORT).show()
                        loadConfigs()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun importConfig() {
        // TODO: 实现从JSON导入配置
        Toast.makeText(this, "功能开发中", Toast.LENGTH_SHORT).show()
    }
    
    private fun editConfig(config: ApiConfig) {
        val intent = Intent(this, ApiConfigEditActivity::class.java).apply {
            putExtra(ApiConfigEditActivity.EXTRA_CONFIG_ID, config.id)
        }
        startActivity(intent)
    }
    
    private fun showDeleteConfirmDialog(config: ApiConfig) {
        AlertDialog.Builder(this)
            .setTitle("删除配置")
            .setMessage("确定要删除 ${config.name} 吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteConfig(config)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun deleteConfig(config: ApiConfig) {
        lifecycleScope.launch {
            repository.deleteApiConfig(config)
            Toast.makeText(this@ApiConfigActivity, "配置已删除", Toast.LENGTH_SHORT).show()
            loadConfigs()
        }
    }
    
    private fun setAsDefault(config: ApiConfig) {
        lifecycleScope.launch {
            repository.setDefaultApiConfig(config.id, config.apiType)
            Toast.makeText(this@ApiConfigActivity, "已设为默认", Toast.LENGTH_SHORT).show()
            loadConfigs()
        }
    }
    
    private fun testConfig(config: ApiConfig) {
        // 根据配置类型进行测试
        when (config.apiType) {
            ApiConfig.TYPE_CHAT -> {
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_API_CONFIG_ID, config.id)
                }
                startActivity(intent)
            }
            ApiConfig.TYPE_IMAGE_TO_TEXT -> {
                Toast.makeText(this, "请在图片详情页测试图片反推", Toast.LENGTH_SHORT).show()
            }
            ApiConfig.TYPE_TEXT_TO_IMAGE -> {
                val intent = Intent(this, ImageGenerationActivity::class.java).apply {
                    putExtra(ImageGenerationActivity.EXTRA_API_CONFIG_ID, config.id)
                }
                startActivity(intent)
            }
            ApiConfig.TYPE_VIDEO -> {
                val intent = Intent(this, VideoGenerationActivity::class.java).apply {
                    putExtra(VideoGenerationActivity.EXTRA_API_CONFIG_ID, config.id)
                }
                startActivity(intent)
            }
        }
    }
    
    private fun loadConfigs() {
        lifecycleScope.launch {
            val configs = repository.getAllApiConfigsSync()
            adapter.submitList(configs)
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadConfigs()
    }
}

/**
 * API配置适配器
 */
class ApiConfigAdapter(
    private val onEditClick: (ApiConfig) -> Unit,
    private val onDeleteClick: (ApiConfig) -> Unit,
    private val onSetDefaultClick: (ApiConfig) -> Unit,
    private val onTestClick: (ApiConfig) -> Unit
) : RecyclerView.Adapter<ApiConfigAdapter.ViewHolder>() {
    
    private var configs: List<ApiConfig> = emptyList()
    
    fun submitList(newConfigs: List<ApiConfig>) {
        configs = newConfigs
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_api_config, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(configs[position])
    }
    
    override fun getItemCount(): Int = configs.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvModel: TextView = itemView.findViewById(R.id.tvModel)
        private val tvUrl: TextView = itemView.findViewById(R.id.tvUrl)
        private val ivDefault: ImageView = itemView.findViewById(R.id.ivDefault)
        private val btnMore: MaterialButton = itemView.findViewById(R.id.btnMore)
        
        fun bind(config: ApiConfig) {
            tvName.text = config.name
            tvType.text = ApiConfig.getTypeName(config.apiType)
            tvModel.text = config.model
            tvUrl.text = config.baseUrl
            
            ivDefault.visibility = if (config.isDefault) View.VISIBLE else View.GONE
            
            btnMore.setOnClickListener {
                showPopupMenu(it, config)
            }
            
            itemView.setOnClickListener {
                onTestClick(config)
            }
        }
        
        private fun showPopupMenu(view: View, config: ApiConfig) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_api_config, popup.menu)
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        onEditClick(config)
                        true
                    }
                    R.id.action_set_default -> {
                        onSetDefaultClick(config)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(config)
                        true
                    }
                    else -> false
                }
            }
            
            popup.show()
        }
    }
}
