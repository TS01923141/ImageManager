package com.example.imagemanager.view.album

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.imagemanager.databinding.ItemImageBinding
import com.example.imagemanager.model.MediaStoreImage

class AlbumAdapter(val onClick: (MediaStoreImage) -> Unit) :
    ListAdapter<MediaStoreImage, RecyclerView.ViewHolder>(MediaStoreImage.DiffCallback){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return AlbumViewHolder(ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) (holder as AlbumViewHolder).bind(item)
    }

    class AlbumViewHolder(private val binding: ItemImageBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaStoreImage) {
            binding.imageViewImage.load(item.contentUri)
        }
    }
}