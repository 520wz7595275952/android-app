package com.promptimagemanager.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.promptimagemanager.R
import com.promptimagemanager.data.Category

class CategoryAdapter(
    private val onItemClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : ListAdapter<CategoryWithCount, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
    
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvCount: TextView = itemView.findViewById(R.id.tvCount)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        
        fun bind(item: CategoryWithCount) {
            tvName.text = item.category.name
            tvCount.text = "${item.count} 张图片"
            
            itemView.setOnClickListener { onItemClick(item.category) }
            btnDelete.setOnClickListener { onDeleteClick(item.category) }
        }
    }
    
    class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryWithCount>() {
        override fun areItemsTheSame(oldItem: CategoryWithCount, newItem: CategoryWithCount): Boolean {
            return oldItem.category.id == newItem.category.id
        }
        
        override fun areContentsTheSame(oldItem: CategoryWithCount, newItem: CategoryWithCount): Boolean {
            return oldItem == newItem
        }
    }
}

data class CategoryWithCount(
    val category: Category,
    val count: Int
)
