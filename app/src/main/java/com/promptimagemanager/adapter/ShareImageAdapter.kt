package com.promptimagemanager.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.promptimagemanager.R

class ShareImageAdapter : RecyclerView.Adapter<ShareImageAdapter.ImageViewHolder>() {
    
    private val images = mutableListOf<Uri>()
    
    fun setImages(uris: List<Uri>) {
        images.clear()
        images.addAll(uris)
        notifyDataSetChanged()
    }
    
    fun getImages(): List<Uri> = images.toList()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_share_image, parent, false)
        return ImageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }
    
    override fun getItemCount(): Int = images.size
    
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        
        fun bind(uri: Uri) {
            Glide.with(itemView.context)
                .load(uri)
                .centerCrop()
                .into(ivImage)
        }
    }
}
