package com.promptimagemanager.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object ClipboardHelper {
    
    fun copyText(context: Context, text: String, label: String = "Prompt") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
}
