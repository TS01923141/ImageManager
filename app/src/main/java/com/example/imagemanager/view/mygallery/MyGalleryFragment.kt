package com.example.imagemanager.view.mygallery

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.imagemanager.databinding.FragmentMyGalleryBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File
import android.content.Intent


/*

 */
class MyGalleryFragment: Fragment() {
    private val viewModel : MyGalleryViewModel by viewModels()
    private var _binding : FragmentMyGalleryBinding? = null
    private val binding get() = _binding!!
    private lateinit var documentPickerResult : ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        documentPickerResult = registerDocumentPickerResult()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMyGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            recyclerViewMyGallery.adapter = MyGalleryAdapter() {
                //開啟圖片
                viewImageUsingExternalApp(it)
                //分享圖片
//                shareImage(it)
            }
            buttonMyGallery.setOnClickListener {
                openDocumentPicker()
            }
        }
        with(viewModel) {
            notification.observe(this@MyGalleryFragment) {
                //應該不用notification，直接選完圖片刷新就好
                viewModel.loadImages()
            }
            images.observe(this@MyGalleryFragment) {
                if (images.value == null) return@observe
                (binding.recyclerViewMyGallery.adapter as MyGalleryAdapter).submitList(images.value)
            }
        }
        viewModel.loadImages()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun registerDocumentPickerResult(): ActivityResultLauncher<Intent> {
        /**
         * registerForActivityResult要在Start前register，在Created後才能使用
         *
         * 使用[ContentResolver.takePersistableUriPermission]讓APP有權限使用Uri直到Activity結束
         *
         * Upon getting a document uri returned, we can use
         * [ContentResolver.takePersistableUriPermission] in order to persist the
         * permission across restarts.
         *
         * This may not be necessary for your app. If the permission is not
         * persisted, access to the uri is granted until the receiving Activity is
         * finished. You can extend the lifetime of the permission grant by passing
         * it along to another Android component. This is done by including the uri
         * in the data field or the ClipData object of the Intent used to launch that
         * component. Additionally, you need to add FLAG_GRANT_READ_URI_PERMISSION
         * and/or FLAG_GRANT_WRITE_URI_PERMISSION to the Intent.
         *
         * This app takes the persistable URI permission grant to demonstrate how, and
         * to allow us to reopen the last opened document when the app starts.
         */
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result?.data?.data.also { documentUri ->
                    if (documentUri == null) return@registerForActivityResult
                    requireContext().contentResolver.takePersistableUriPermission(
                        documentUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    viewModel.copyImageFromUri(documentUri)
                }
            }
        }
    }

    private fun openDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            /**
             * 限定mime type
             * It's possible to limit the types of files by mime-type. Since this
             * app displays pages from a PDF file, we'll specify `application/pdf`
             * in `type`.
             * See [Intent.setType] for more details.
             */
            type = "image/*"

            /**
             * 設定[Intent.CATEGORY_OPENABLE]以確保使用[ContentResolver.openFileDescriptor]開啟檔案成功
             * Because we'll want to use [ContentResolver.openFileDescriptor] to read
             * the data of whatever file is picked, we set [Intent.CATEGORY_OPENABLE]
             * to ensure this will succeed.
             */
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        documentPickerResult.launch(intent)
    }

    //呼叫外部app開啟圖片
    private fun viewImageUsingExternalApp(imageFile: File) {
        val context = requireContext()
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, imageFile)

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            data = contentUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, "Couldn't find suitable app to display the image", Snackbar.LENGTH_SHORT).show()
        }
    }

    //分享圖片
    private fun shareImage(imageFile: File) {
        val context = requireContext()
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, imageFile)

        val i = Intent(Intent.ACTION_SEND)
        i.type = "image/*"
        i.putExtra(Intent.EXTRA_STREAM, contentUri)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(i, "Share Image"))
    }
}