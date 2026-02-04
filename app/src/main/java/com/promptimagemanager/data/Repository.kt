package com.promptimagemanager.data

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Repository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val imageDao = database.imageDao()
    private val categoryDao = database.categoryDao()
    private val apiConfigDao = database.apiConfigDao()
    
    // Image operations
    fun getAllImages(): LiveData<List<ImageItem>> = imageDao.getAllImages()
    
    fun getImagesByCategory(categoryId: Long): LiveData<List<ImageItem>> = 
        imageDao.getImagesByCategory(categoryId)
    
    fun searchImages(query: String): LiveData<List<ImageItem>> = 
        imageDao.searchImages(query)
    
    suspend fun getImageById(id: Long): ImageItem? = 
        withContext(Dispatchers.IO) {
            imageDao.getImageById(id)
        }
    
    suspend fun insertImage(image: ImageItem): Long = 
        withContext(Dispatchers.IO) {
            imageDao.insertImage(image)
        }
    
    suspend fun updateImage(image: ImageItem) = 
        withContext(Dispatchers.IO) {
            imageDao.updateImage(image)
        }
    
    suspend fun deleteImage(image: ImageItem) = 
        withContext(Dispatchers.IO) {
            imageDao.deleteImage(image)
        }
    
    suspend fun getImageCountByCategory(categoryId: Long): Int = 
        withContext(Dispatchers.IO) {
            imageDao.getImageCountByCategory(categoryId)
        }
    
    // Category operations
    fun getAllCategories(): LiveData<List<Category>> = categoryDao.getAllCategories()
    
    suspend fun getAllCategoriesSync(): List<Category> = 
        withContext(Dispatchers.IO) {
            categoryDao.getAllCategoriesSync()
        }
    
    suspend fun getCategoryById(id: Long): Category? = 
        withContext(Dispatchers.IO) {
            categoryDao.getCategoryById(id)
        }
    
    suspend fun insertCategory(category: Category): Long = 
        withContext(Dispatchers.IO) {
            categoryDao.insertCategory(category)
        }
    
    suspend fun updateCategory(category: Category) = 
        withContext(Dispatchers.IO) {
            categoryDao.updateCategory(category)
        }
    
    suspend fun deleteCategory(category: Category) = 
        withContext(Dispatchers.IO) {
            categoryDao.deleteCategory(category)
        }
    
    suspend fun categoryExists(name: String): Boolean = 
        withContext(Dispatchers.IO) {
            categoryDao.countByName(name) > 0
        }
    
    // API Config operations
    fun getAllApiConfigs(): LiveData<List<ApiConfig>> = apiConfigDao.getAllConfigs()
    
    suspend fun getAllApiConfigsSync(): List<ApiConfig> = 
        withContext(Dispatchers.IO) {
            apiConfigDao.getAllConfigsSync()
        }
    
    fun getApiConfigsByType(type: Int): LiveData<List<ApiConfig>> = 
        apiConfigDao.getConfigsByType(type)
    
    suspend fun getApiConfigsByTypeSync(type: Int): List<ApiConfig> = 
        withContext(Dispatchers.IO) {
            apiConfigDao.getConfigsByTypeSync(type)
        }
    
    suspend fun getApiConfigById(id: Long): ApiConfig? = 
        withContext(Dispatchers.IO) {
            apiConfigDao.getConfigById(id)
        }
    
    suspend fun getDefaultApiConfig(type: Int): ApiConfig? = 
        withContext(Dispatchers.IO) {
            apiConfigDao.getDefaultConfig(type)
        }
    
    suspend fun insertApiConfig(config: ApiConfig): Long = 
        withContext(Dispatchers.IO) {
            apiConfigDao.insertConfig(config)
        }
    
    suspend fun updateApiConfig(config: ApiConfig) = 
        withContext(Dispatchers.IO) {
            apiConfigDao.updateConfig(config)
        }
    
    suspend fun deleteApiConfig(config: ApiConfig) = 
        withContext(Dispatchers.IO) {
            apiConfigDao.deleteConfig(config)
        }
    
    suspend fun setDefaultApiConfig(id: Long, type: Int) = 
        withContext(Dispatchers.IO) {
            apiConfigDao.setDefaultConfig(id, type)
        }
}
