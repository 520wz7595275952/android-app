package com.promptimagemanager.data

import androidx.room.Embedded
import androidx.room.Relation

data class ImageWithCategory(
    @Embedded
    val image: ImageItem,
    
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: Category?
)
