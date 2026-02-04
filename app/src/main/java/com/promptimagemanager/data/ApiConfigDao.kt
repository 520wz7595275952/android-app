package com.promptimagemanager.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ApiConfigDao {
    
    @Query("SELECT * FROM api_configs ORDER BY createdAt DESC")
    fun getAllConfigs(): LiveData<List<ApiConfig>>
    
    @Query("SELECT * FROM api_configs ORDER BY createdAt DESC")
    suspend fun getAllConfigsSync(): List<ApiConfig>
    
    @Query("SELECT * FROM api_configs WHERE apiType = :type ORDER BY createdAt DESC")
    fun getConfigsByType(type: Int): LiveData<List<ApiConfig>>
    
    @Query("SELECT * FROM api_configs WHERE apiType = :type ORDER BY createdAt DESC")
    suspend fun getConfigsByTypeSync(type: Int): List<ApiConfig>
    
    @Query("SELECT * FROM api_configs WHERE id = :id")
    suspend fun getConfigById(id: Long): ApiConfig?
    
    @Query("SELECT * FROM api_configs WHERE isDefault = 1 AND apiType = :type LIMIT 1")
    suspend fun getDefaultConfig(type: Int): ApiConfig?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ApiConfig): Long
    
    @Update
    suspend fun updateConfig(config: ApiConfig)
    
    @Delete
    suspend fun deleteConfig(config: ApiConfig)
    
    @Query("UPDATE api_configs SET isDefault = 0 WHERE apiType = :type")
    suspend fun clearDefault(type: Int)
    
    @Query("UPDATE api_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setAsDefault(id: Long)
    
    @Transaction
    suspend fun setDefaultConfig(id: Long, type: Int) {
        clearDefault(type)
        setAsDefault(id)
    }
}
