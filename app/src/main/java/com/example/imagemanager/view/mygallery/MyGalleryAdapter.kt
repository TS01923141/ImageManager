package com.example.imagemanager.view.mygallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.imagemanager.databinding.ItemImageBinding
import java.io.File

class MyGalleryAdapter(private val onClick: (File) -> Unit): ListAdapter<File, MyGalleryAdapter.ImageViewHolder>(ListItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(ItemImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(private val binding: ItemImageBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: File) {
            binding.apply {
                imageViewImage.load(item) {
                    //淡入加載效果
                    crossfade(true)
                }
                constraintLayoutImageFrame.setOnClickListener {
                    onClick.invoke(item)
                }
            }
        }
    }
}

class ListItemCallback: DiffUtil.ItemCallback<File>() {
    override fun areItemsTheSame(oldItem: File, newItem: File): Boolean =
        oldItem.name == newItem.name

    override fun areContentsTheSame(oldItem: File, newItem: File) =
        oldItem == newItem
}