package com.example.imagemanager.view.album

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.imagemanager.databinding.FragmentAlbumBinding

class AlbumFragment: Fragment() {
    private val viewModel : AlbumViewModel by viewModels()
    private var _binding : FragmentAlbumBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerViewAlbum.apply {
            adapter = AlbumAdapter() {

            }
        }

        with(viewModel){
            images.observe(this@AlbumFragment, { images ->
                (binding.recyclerViewAlbum.adapter as AlbumAdapter).submitList(images)})
        }
        viewModel.loadImages()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}