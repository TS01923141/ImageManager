package com.example.imagemanager.view.mygallery

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.content.res.XmlResourceParser
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.*
import com.example.imagemanager.R
import com.example.imagemanager.model.MediaStoreImage
import com.example.imagemanager.model.utils.copyImageFromStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

private const val FILEPATH_XML_KEY = "files-path"
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
}

private fun getImagesFolder(context: Context): File {
    //取得xml
    val xml = context.resources.getXml(R.xml.filepaths)

    //取得所有FILEPATH_XML_KEY在xml內的參數
    val attributes = getAttributesFromXmlNode(xml, FILEPATH_XML_KEY)

    val folderPath = attributes["path"]
        ?: error("You have to specify the sharable directory in res/xml/filepaths.xml")

    //建立並回傳資料夾File
    return File(context.filesDir, folderPath).also {
        if (!it.exists()) {
            it.mkdir()
        }
    }
}

//取得所有nodeName的參數
// TODO: Make the function suspend
private fun getAttributesFromXmlNode(
    xml: XmlResourceParser,
    nodeName: String
): Map<String, String> {
    //非end tag
    while (xml.eventType != XmlResourceParser.END_DOCUMENT) {
        //是start tag
        if (xml.eventType == XmlResourceParser.START_TAG) {
            //tag name是nodeName
            if (xml.name == nodeName) {
                //判斷參數數量
                if (xml.attributeCount == 0) {
                    return emptyMap()
                }

                val attributes = mutableMapOf<String, String>()
                //參數寫入map
                for (index in 0 until xml.attributeCount) {
                    attributes[xml.getAttributeName(index)] = xml.getAttributeValue(index)
                }

                return attributes
            }
        }

        xml.next()
    }

    return emptyMap()
}