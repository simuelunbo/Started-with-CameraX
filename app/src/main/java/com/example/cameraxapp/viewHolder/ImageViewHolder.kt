package com.example.cameraxapp.viewHolder

import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import com.example.cameraxapp.databinding.ViewholderImageBinding
import com.example.cameraxapp.extensions.loadCenterCrop

class ImageViewHolder(private val binding: ViewholderImageBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bindData(uri: Uri) = with(binding) {
        imageView.loadCenterCrop(uri.toString())
    }

}