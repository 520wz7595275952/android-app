package com.promptimagemanager.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.promptimagemanager.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.promptimagemanager.data.ApiConfig
import com.promptimagemanager.data.Repository
import kotlinx.coroutines.launch

/**
 * API配置编辑界面
 */
class ApiConfigEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONFIG_ID = "extra_config_id"
    }
    
    private var configId: Long = -1
    private var currentConfig: ApiConfig? = null
    private lateinit var repository: Repository
    
    private lateinit var etName: TextInputEditText
    private lateinit var spinnerType: MaterialAutoCompleteTextView
    private lateinit var etBaseUrl: TextInputEditText
    private lateinit var etApiKey: TextInputEditText
    private lateinit var etModel: TextInputEditText
    private lateinit var etHeaders: TextInputEditText
    private lateinit var etParams: TextInputEditText
    private lateinit var etTimeout: TextInputEditText
    private lateinit var btnSave: MaterialButton
    
    private val apiTypes = listOf(
        "文字聊天" to ApiConfig.TYPE_CHAT,
        "图片反推" to ApiConfig.TYPE_IMAGE_TO_TEXT,
        "图片生成" to ApiConfig.TYPE_TEXT_TO_IMAGE,
        "视频生成" to ApiConfig.TYPE_VIDEO
    )
    
    private var selectedType: Int = ApiConfig.TYPE_CHAT
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_config_edit)
        
        repository = Repository(this)
        configId = intent.getLongExtra(EXTRA_CONFIG_ID, -1)
        
        initViews()
        setupToolbar()
        setupTypeSpinner()
        
        if (configId != -1L) {
            loadConfig()
        }
        
        btnSave.setOnClickListener {
            saveConfig()
        }
    }
    
    private fun initViews() {
        etName = findViewById(R.id.etName)
        spinnerType = findViewById(R.id.spinnerType)
        etBaseUrl = findViewById(R.id.etBaseUrl)
        etApiKey = findViewById(R.id.etApiKey)
        etModel = findViewById(R.id.etModel)
        etHeaders = findViewById(R.id.etHeaders)
        etParams = findViewById(R.id.etParams)
        etTimeout = findViewById(R.id.etTimeout)
        btnSave = findViewById(R.id.btnSave)
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (configId == -1L) "添加API配置" else "编辑API配置"
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupTypeSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            apiTypes.map { it.first }
        )
        spinnerType.setAdapter(adapter)
        
        spinnerType.setOnItemClickListener { _, _, position, _ ->
            selectedType = apiTypes[position].second
        }
        
        // 默认选择第一个
        spinnerType.setText(apiTypes[0].first, false)
    }
    
    private fun loadConfig() {
        lifecycleScope.launch {
            currentConfig = repository.getApiConfigById(configId)
            currentConfig?.let { config ->
                etName.setText(config.name)
                etBaseUrl.setText(config.baseUrl)
                etApiKey.setText(config.apiKey)
                etModel.setText(config.model)
                etHeaders.setText(config.headers)
                etParams.setText(config.params)
                etTimeout.setText(config.timeout.toString())
                
                // 设置类型
                selectedType = config.apiType
                val typeName = apiTypes.find { it.second == config.apiType }?.first ?: apiTypes[0].first
                spinnerType.setText(typeName, false)
            }
        }
    }
    
    private fun saveConfig() {
        val name = etName.text.toString().trim()
        val baseUrl = etBaseUrl.text.toString().trim()
        val apiKey = etApiKey.text.toString().trim()
        val model = etModel.text.toString().trim()
        val headers = etHeaders.text.toString().trim()
        val params = etParams.text.toString().trim()
        val timeout = etTimeout.text.toString().toIntOrNull() ?: 60
        
        if (name.isEmpty() || baseUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
            Toast.makeText(this, "请填写必填项", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            val config = if (currentConfig != null) {
                currentConfig!!.copy(
                    name = name,
                    apiType = selectedType,
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    model = model,
                    headers = headers,
                    params = params,
                    timeout = timeout
                )
            } else {
                ApiConfig(
                    name = name,
                    apiType = selectedType,
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    model = model,
                    headers = headers,
                    params = params,
                    timeout = timeout
                )
            }
            
            repository.insertApiConfig(config)
            Toast.makeText(this@ApiConfigEditActivity, "配置已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
