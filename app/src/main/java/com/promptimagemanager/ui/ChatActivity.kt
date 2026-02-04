package com.promptimagemanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.promptimagemanager.R
import com.promptimagemanager.api.ApiService
import com.promptimagemanager.data.ApiConfig
import com.promptimagemanager.data.Repository
import com.promptimagemanager.utils.ClipboardHelper
import kotlinx.coroutines.launch

/**
 * AI聊天界面
 */
class ChatActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_API_CONFIG_ID = "extra_api_config_id"
    }
    
    private var apiConfigId: Long = -1
    private lateinit var apiConfig: ApiConfig
    private lateinit var repository: Repository
    private lateinit var apiService: ApiService
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ChatAdapter
    
    private val messages = mutableListOf<ChatMessage>()
    
    data class ChatMessage(
        val content: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        apiConfigId = intent.getLongExtra(EXTRA_API_CONFIG_ID, -1)
        if (apiConfigId == -1L) {
            Toast.makeText(this, "未选择API配置", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        repository = Repository(this)
        apiService = ApiService(this)
        
        initViews()
        setupToolbar()
        setupRecyclerView()
        
        loadApiConfig()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        progressBar = findViewById(R.id.progressBar)
        
        btnSend.setOnClickListener {
            sendMessage()
        }
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = ChatAdapter(
            onCopyClick = { message ->
                ClipboardHelper.copyText(this, message.content)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun loadApiConfig() {
        lifecycleScope.launch {
            val config = repository.getApiConfigById(apiConfigId)
            if (config == null) {
                Toast.makeText(this@ChatActivity, "API配置不存在", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            apiConfig = config
            supportActionBar?.title = config.name
        }
    }
    
    private fun sendMessage() {
        val message = etMessage.text.toString().trim()
        if (message.isEmpty()) return
        
        // 添加用户消息
        val userMessage = ChatMessage(content = message, isUser = true)
        messages.add(userMessage)
        adapter.submitList(messages.toList())
        recyclerView.scrollToPosition(messages.size - 1)
        
        etMessage.text.clear()
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val apiMessages = messages.map {
                    ApiService.Companion.ChatMessage(
                        role = if (it.isUser) "user" else "assistant",
                        content = it.content
                    )
                }
                
                val result = apiService.chat(apiConfig, apiMessages)
                
                progressBar.visibility = View.GONE
                
                if (result.success) {
                    val aiMessage = ChatMessage(
                        content = result.data ?: "",
                        isUser = false
                    )
                    messages.add(aiMessage)
                    adapter.submitList(messages.toList())
                    recyclerView.scrollToPosition(messages.size - 1)
                } else {
                    Toast.makeText(
                        this@ChatActivity,
                        "请求失败: ${result.error}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(
                    this@ChatActivity,
                    "错误: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

/**
 * 聊天消息适配器
 */
class ChatAdapter(
    private val onCopyClick: (ChatActivity.ChatMessage) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    
    private var messages: List<ChatActivity.ChatMessage> = emptyList()
    
    fun submitList(newMessages: List<ChatActivity.ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) 1 else 0
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = if (viewType == 1) {
            R.layout.item_chat_user
        } else {
            R.layout.item_chat_ai
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }
    
    override fun getItemCount(): Int = messages.size
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val btnCopy: ImageButton? = itemView.findViewById(R.id.btnCopy)
        
        fun bind(message: ChatActivity.ChatMessage) {
            tvMessage.text = message.content
            
            btnCopy?.setOnClickListener {
                onCopyClick(message)
            }
        }
    }
}
