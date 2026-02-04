package com.promptimagemanager.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ImageDao {
    @Query("SELECT * FROM images ORDER BY createdAt DESC")
    fun getAllImages(): LiveData<List<ImageItem>>
    
    @Query("SELECT * FROM images WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getImagesByCategory(categoryId: Long): LiveData<List<ImageItem>>
    
    @Query("SELECT * FROM images WHERE prompt LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchImages(query: String): LiveData<List<ImageItem>>
    
    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getImageById(id: Long): ImageItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageItem): Long
    
    @Update
    suspend fun updateImage(image: ImageItem)
    
    @Delete
    suspend fun deleteImage(image: ImageItem)
    
    @Query("SELECT COUNT(*) FROM images WHERE categoryId = :categoryId")
    suspend fun getImageCountByCategory(categoryId: Long): Int
}
