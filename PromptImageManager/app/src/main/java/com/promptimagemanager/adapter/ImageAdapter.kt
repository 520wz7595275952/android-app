package com.promptimagemanager.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.promptimagemanager.R
import com.promptimagemanager.data.ImageItem
import com.google.android.material.button.MaterialButton

class ImageAdapter(
    private val onItemClick: (ImageItem) -> Unit,
    private val onCopyClick: (ImageItem) -> Unit,
    private val onEditClick: (ImageItem) -> Unit
) : ListAdapter<ImageItem, ImageAdapter.ImageViewHolder>(ImageDiffCallback()) {
    
    private var categoryMap: Map<Long, String> = emptyMap()
    
    fun setCategoryMap(map: Map<Long, String>) {
        categoryMap = map
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = getItem(position)
        holder.bind(image, categoryMap[image.categoryId])
    }
    
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        private val tvPrompt: TextView = itemView.findViewById(R.id.tvPrompt)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val btnCopy: MaterialButton = itemView.findViewById(R.id.btnCopy)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        
        fun bind(image: ImageItem, categoryName: String?) {
            // Load image with Glide
            Glide.with(itemView.context)
                .load(image.imagePath)
                .placeholder(R.drawable.ic_launcher_foreground)
                .centerCrop()
                .into(ivImage)
            
            // Set prompt text
            tvPrompt.text = image.prompt.ifEmpty { "无提示词" }
            
            // Set category
            if (categoryName != null) {
                tvCategory.text = categoryName
                tvCategory.visibility = View.VISIBLE
            } else {
                tvCategory.text = "未分类"
                tvCategory.visibility = View.VISIBLE
            }
            
            // Click listeners
            itemView.setOnClickListener { onItemClick(image) }
            btnCopy.setOnClickListener { onCopyClick(image) }
            btnEdit.setOnClickListener { onEditClick(image) }
        }
    }
    
    class ImageDiffCallback : DiffUtil.ItemCallback<ImageItem>() {
        override fun areItemsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ImageItem, newItem: ImageItem): Boolean {
            return oldItem == newItem
        }
    }
}
