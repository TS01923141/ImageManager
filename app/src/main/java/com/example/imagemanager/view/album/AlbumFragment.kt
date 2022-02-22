package com.example.imagemanager.view.album

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.imagemanager.R
import com.example.imagemanager.databinding.FragmentAlbumBinding
import com.example.imagemanager.model.MediaStoreImage
import com.example.imagemanager.model.extend.checkAndRequestPermission
import com.example.imagemanager.model.permission.PermissionRequestFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

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
//                deleteImage(it)
                shareImage(it.contentUri)
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


    //跳dialog，確定刪除的話交由viewModel.deleteImage()處理
    private fun deleteImage(image: MediaStoreImage) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_dialog_title)
            .setMessage(getString(R.string.delete_dialog_message, image.displayName))
            .setPositiveButton(R.string.delete_dialog_positive) { _: DialogInterface, _: Int ->
                viewModel.deleteImage(image)
            }
            .setNegativeButton(R.string.delete_dialog_negative) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    //分享圖片
    private fun shareImage(imageUri: Uri) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "image/*"
        i.putExtra(Intent.EXTRA_STREAM, imageUri)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(i, "Share Image"))
    }
}