package com.example.imagemanager.view.mygallery

import android.app.Application
import android.content.Context
import android.content.res.XmlResourceParser
import android.net.Uri
import androidx.lifecycle.*
import com.example.imagemanager.model.utils.copyImageFromStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

class MyGalleryViewModel(app: Application): AndroidViewModel(app) {

    private val context: Context get() = getApplication()
    private val imagesFolder: File by lazy { getImagesFolder(context) }
    private val _notification = MutableLiveData<String>()
    val notification: LiveData<String> get() = _notification
    private val _images = MutableLiveData(emptyList<File>())
    val images: LiveData<List<File>> get() = _images

    //複製圖片並更新liveData
    fun copyImageFromUri(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.let {
                    // TODO: Apply grayscale filter before saving image
                    copyImageFromStream(it, imagesFolder)
                    _notification.postValue("Image copied")
                }
            }
        }
    }

    //從io thread取得imagesFolder內的所有file並更新liveData
    fun loadImages() {
        viewModelScope.launch {
            val images = withContext(Dispatchers.IO) {
                imagesFolder.listFiles().toList()
            }

            _images.postValue(images)
        }
    }

    private fun getImagesFolder(context: Context): File {
        //建立並回傳資料夾File
        return File(context.filesDir, "images/").also {
            if (!it.exists()) {
                it.mkdir()
            }
        }
    }

    fun refreshImages() {

    }
}