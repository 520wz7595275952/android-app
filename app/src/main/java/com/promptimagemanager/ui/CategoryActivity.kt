package com.promptimagemanager.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.promptimagemanager.R
import com.promptimagemanager.adapter.CategoryAdapter
import com.promptimagemanager.adapter.CategoryWithCount
import com.promptimagemanager.data.Category
import com.promptimagemanager.data.Repository
import com.promptimagemanager.databinding.ActivityCategoryBinding
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding
    private lateinit var repository: Repository
    private lateinit var categoryAdapter: CategoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        repository = Repository(this)
        
        setupToolbar()
        setupRecyclerView()
        setupFab()
        
        loadCategories()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onItemClick = { category ->
                // Return to main with selected category
                finish()
            },
            onDeleteClick = { category ->
                showDeleteConfirmDialog(category)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CategoryActivity)
            adapter = categoryAdapter
        }
    }
    
    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            showAddCategoryDialog()
        }
    }
    
    private fun loadCategories() {
        lifecycleScope.launch {
            val categories = repository.getAllCategoriesSync()
            val categoryWithCounts = categories.map { category ->
                val count = repository.getImageCountByCategory(category.id)
                CategoryWithCount(category, count)
            }
            categoryAdapter.submitList(categoryWithCounts)
        }
    }
    
    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    addCategory(name)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun addCategory(name: String) {
        lifecycleScope.launch {
            if (repository.categoryExists(name)) {
                Toast.makeText(this@CategoryActivity, "分类已存在", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val category = Category(name = name)
            repository.insertCategory(category)
            Toast.makeText(this@CategoryActivity, "分类已添加", Toast.LENGTH_SHORT).show()
            loadCategories()
        }
    }
    
    private fun showDeleteConfirmDialog(category: Category) {
        lifecycleScope.launch {
            val count = repository.getImageCountByCategory(category.id)
            val message = if (count > 0) {
                "该分类下有 $count 张图片，删除后这些图片将变为未分类。确定要删除吗？"
            } else {
                "确定要删除这个分类吗？"
            }
            
            AlertDialog.Builder(this@CategoryActivity)
                .setTitle("删除分类")
                .setMessage(message)
                .setPositiveButton("删除") { _, _ ->
                    deleteCategory(category)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
    
    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            repository.deleteCategory(category)
            Toast.makeText(this@CategoryActivity, "分类已删除", Toast.LENGTH_SHORT).show()
            loadCategories()
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
