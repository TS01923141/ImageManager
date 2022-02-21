package com.example.imagemanager.view.album

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.imagemanager.databinding.FragmentAlbumBinding
import com.example.imagemanager.model.extend.checkAndRequestPermission
import com.example.imagemanager.model.permission.PermissionRequestFragment

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
        //check permission and load data
        val list = arrayListOf(
            //依照sdk版本決定要哪個讀取的權限
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                PermissionRequestFragment.MANAGE_EXTERNAL_STORAGE_PERMISSION
                else PermissionRequestFragment.READ_EXTERNAL_STORAGE
        )
        requireActivity().checkAndRequestPermission(list) {
            if (it) viewModel.loadImages()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}