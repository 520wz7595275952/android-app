package com.promptimagemanager.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

class ImageStorageManager(private val context: Context) {
    
    companion object {
        private const val DIRECTORY_NAME = "PromptImages"
        private const val NOMEDIA_FILE = ".nomedia"
    }
    
    /**
     * Save image to private app storage (not visible in gallery)
     * Uses app-private directory which is automatically excluded from gallery
     */
    suspend fun saveImageToPrivateStorage(uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open input stream")
        
        // Use app-private files directory - automatically hidden from gallery
        val imagesDir = File(context.filesDir, DIRECTORY_NAME)
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val outputFile = File(imagesDir, fileName)
        
        inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
        
        outputFile.absolutePath
    }
    
    /**
     * Save bitmap to private storage
     */
    suspend fun saveBitmapToPrivateStorage(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, DIRECTORY_NAME)
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val outputFile = File(imagesDir, fileName)
        
        FileOutputStream(outputFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
        
        outputFile.absolutePath
    }
    
    /**
     * Delete image file
     */
    suspend fun deleteImage(imagePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(imagePath)
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
    
    /**
     * Load bitmap from file
     */
    suspend fun loadBitmap(imagePath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeFile(imagePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get file from Uri
     */
    suspend fun getFileFromUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val tempFile = File.createTempFile("temp", ".jpg", context.cacheDir)
            
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Copy image from external URI to private storage
     */
    suspend fun copyImageToPrivateStorage(sourceUri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalArgumentException("Cannot open input stream")
        
        val imagesDir = File(context.filesDir, DIRECTORY_NAME)
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val outputFile = File(imagesDir, fileName)
        
        inputStream.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
        
        outputFile.absolutePath
    }
}
