package com.example.cameraxapp.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.cameraxapp.databinding.ViewholderImageBinding
import com.example.cameraxapp.extensions.loadCenterCrop
import com.example.cameraxapp.viewHolder.ImageViewHolder

class ImageViewPagerAdapter(
    var uriList: List<Uri>
) : RecyclerView.Adapter<ImageViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageViewHolder {
        val binding = ViewholderImageBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bindData(uriList[position])
    }

    override fun getItemCount(): Int = uriList.count()



}