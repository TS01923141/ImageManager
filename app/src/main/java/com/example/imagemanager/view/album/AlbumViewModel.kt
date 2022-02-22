package com.example.imagemanager.view.album

import android.annotation.SuppressLint
import android.app.Application
import android.app.RecoverableSecurityException
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.*
import com.example.imagemanager.model.MediaStoreImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "AlbumViewModel"
@HiltViewModel
class AlbumViewModel @Inject constructor(app: Application): AndroidViewModel(app) {
    private val _images = MutableLiveData<List<MediaStoreImage>>()
    val images: LiveData<List<MediaStoreImage>> get() = _images

    private var contentObserver: ContentObserver? = null

    private var pendingDeleteImage: MediaStoreImage? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()
    val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete

    /**
     * 讀圖與設定內容改變時自動讀圖加載
     *
     * Performs a one shot load of images from [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] into
     * the [_images] [LiveData] above.
     */
    fun loadImages() {
        viewModelScope.launch {
            //query
            val imageList = queryImages()
            _images.postValue(imageList)

            if (contentObserver == null) {
                //觀察[MediaStore.Images.Media.EXTERNAL_CONTENT_URI]，當內容有改變時loadImages
                contentObserver = getApplication<Application>().contentResolver.registerObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ) {
                    loadImages()
                }
            }
        }
    }

    private suspend fun queryImages(): List<MediaStoreImage> {
        val images = mutableListOf<MediaStoreImage>()

        /**
         * Working with [ContentResolver]s can be slow, so we'll do this off the main
         * thread inside a coroutine.
         */
        withContext(Dispatchers.IO) {

            /**
             * 使用[ContentProvider]時常會需要用到"projections"，projections類似SQL的"SELECT..."，
             * projections內是我們想要的資料的空columns list，透過request去裝填資料
             *
             * 如果[ContentResolver.query]內projections非必須可以傳null，如果需求數據太多可能會影響效能
             *
             * A key concept when working with Android [ContentProvider]s is something called
             * "projections". A projection is the list of columns to request from the provider,
             * and can be thought of (quite accurately) as the "SELECT ..." clause of a SQL
             * statement.
             *
             * It's not _required_ to provide a projection. In this case, one could pass `null`
             * in place of `projection` in the call to [ContentResolver.query], but requesting
             * more data than is required has a performance impact.
             *
             * For this sample, we only use a few columns of data, and so we'll request just a
             * subset of columns.
             */
            /**
             * 使用[ContentProvider]時常會需要用到"projections"，projections類似SQL的"SELECT..."，
             * projections內是我們想要的資料的空columns list，透過request去裝填資料
             *
             * 如果[ContentResolver.query]內projections非必須可以傳null，如果需求數據太多可能會影響效能
             *
             * A key concept when working with Android [ContentProvider]s is something called
             * "projections". A projection is the list of columns to request from the provider,
             * and can be thought of (quite accurately) as the "SELECT ..." clause of a SQL
             * statement.
             *
             * It's not _required_ to provide a projection. In this case, one could pass `null`
             * in place of `projection` in the call to [ContentResolver.query], but requesting
             * more data than is required has a performance impact.
             *
             * For this sample, we only use a few columns of data, and so we'll request just a
             * subset of columns.
             */

            //需要的資料
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            /**
             * selection類似SQL的"WHERE..."，一樣可以傳null，此時會回傳全部資料
             * 此處要求限定有date的圖片
             *
             * 此處有加`?`，這個值會由selectionArgs提供
             *
             * The `selection` is the "WHERE ..." clause of a SQL statement. It's also possible
             * to omit this by passing `null` in its place, and then all rows will be returned.
             * In this case we're using a selection based on the date the image was taken.
             *
             * Note that we've included a `?` in our selection. This stands in for a variable
             * which will be provided by the next variable.
             */
            /**
             * selection類似SQL的"WHERE..."，一樣可以傳null，此時會回傳全部資料
             * 此處要求限定有date的圖片
             *
             * 此處有加`?`，這個值會由selectionArgs提供
             *
             * The `selection` is the "WHERE ..." clause of a SQL statement. It's also possible
             * to omit this by passing `null` in its place, and then all rows will be returned.
             * In this case we're using a selection based on the date the image was taken.
             *
             * Note that we've included a `?` in our selection. This stands in for a variable
             * which will be provided by the next variable.
             */

            //filter條件
            val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"

            /**
             * selectionArgs是一個list，他會填入每個`selection`內的`?`
             *
             * The `selectionArgs` is a list of values that will be filled in for each `?`
             * in the `selection`.
             */
            /**
             * selectionArgs是一個list，他會填入每個`selection`內的`?`
             *
             * The `selectionArgs` is a list of values that will be filled in for each `?`
             * in the `selection`.
             */

            //上面條件的補充參數
            val selectionArgs = arrayOf(
                // Release day of the G1. :)
                dateToTimestamp(day = 22, month = 10, year = 2008).toString()
            )

            /**
             * 就sort
             *
             * Sort order to use. This can also be null, which will use the default sort
             * order. For [MediaStore.Images], the default sort order is ascending by date taken.
             */
            /**
             * 就sort
             *
             * Sort order to use. This can also be null, which will use the default sort
             * order. For [MediaStore.Images], the default sort order is ascending by date taken.
             */

            //就sort
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            //query
            getApplication<Application>().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                /**
                 * 為了從回傳的[Cursor]取回我們感興趣的資料，有兩種方式
                 * 1.[Cursor.getColumnIndex]，如果沒有找到需求id的資料會回傳-1
                 * 2.[Cursor.getColumnIndexOrThrow]，直接取得我們指定的index，
                 *   如果有錯他會拋出[IllegalArgumentException]
                 * 儘量避免單行比對
                 *
                 *
                 * In order to retrieve the data from the [Cursor] that's returned, we need to
                 * find which index matches each column that we're interested in.
                 *
                 * There are two ways to do this. The first is to use the method
                 * [Cursor.getColumnIndex] which returns -1 if the column ID isn't found. This
                 * is useful if the code is programmatically choosing which columns to request,
                 * but would like to use a single method to parse them into objects.
                 *
                 * In our case, since we know exactly which columns we'd like, and we know
                 * that they must be included (since they're all supported from API 1), we'll
                 * use [Cursor.getColumnIndexOrThrow]. This method will throw an
                 * [IllegalArgumentException] if the column named isn't found.
                 *
                 * In either case, while this method isn't slow, we'll want to cache the results
                 * to avoid having to look them up for each row.
                 */
                /**
                 * 為了從回傳的[Cursor]取回我們感興趣的資料，有兩種方式
                 * 1.[Cursor.getColumnIndex]，如果沒有找到需求id的資料會回傳-1
                 * 2.[Cursor.getColumnIndexOrThrow]，直接取得我們指定的index，
                 *   如果有錯他會拋出[IllegalArgumentException]
                 * 儘量避免單行比對
                 *
                 *
                 * In order to retrieve the data from the [Cursor] that's returned, we need to
                 * find which index matches each column that we're interested in.
                 *
                 * There are two ways to do this. The first is to use the method
                 * [Cursor.getColumnIndex] which returns -1 if the column ID isn't found. This
                 * is useful if the code is programmatically choosing which columns to request,
                 * but would like to use a single method to parse them into objects.
                 *
                 * In our case, since we know exactly which columns we'd like, and we know
                 * that they must be included (since they're all supported from API 1), we'll
                 * use [Cursor.getColumnIndexOrThrow]. This method will throw an
                 * [IllegalArgumentException] if the column named isn't found.
                 *
                 * In either case, while this method isn't slow, we'll want to cache the results
                 * to avoid having to look them up for each row.
                 */

                //取得query後我們要的資料
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                Log.i(TAG, "Found ${cursor.count} images")
                while (cursor.moveToNext()) {

                    //取得資料
                    // Here we'll use the column indexs that we found above.
                    val id = cursor.getLong(idColumn)
                    val dateModified =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val displayName = cursor.getString(displayNameColumn)


                    /**
                     * 我們通過[MediaStore.Images.Media.EXTERNAL_CONTENT_URI]訪問圖片們，我們現在有
                     * base URI 跟額外的image ID
                     * 要query影片或音檔也是一樣的方式，透過以下方式取得檔案的Uri
                     *
                     * This is one of the trickiest parts:
                     *
                     * Since we're accessing images (using
                     * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI], we'll use that
                     * as the base URI and append the ID of the image to it.
                     *
                     * This is the exact same way to do it when working with [MediaStore.Video] and
                     * [MediaStore.Audio] as well. Whatever `Media.EXTERNAL_CONTENT_URI` you
                     * query to get the items is the base, and the ID is the document to
                     * request there.
                     */
                    /**
                     * 我們通過[MediaStore.Images.Media.EXTERNAL_CONTENT_URI]訪問圖片們，我們現在有
                     * base URI 跟額外的image ID
                     * 要query影片或音檔也是一樣的方式，透過以下方式取得檔案的Uri
                     *
                     * This is one of the trickiest parts:
                     *
                     * Since we're accessing images (using
                     * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI], we'll use that
                     * as the base URI and append the ID of the image to it.
                     *
                     * This is the exact same way to do it when working with [MediaStore.Video] and
                     * [MediaStore.Audio] as well. Whatever `Media.EXTERNAL_CONTENT_URI` you
                     * query to get the items is the base, and the ID is the document to
                     * request there.
                     */
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    //轉成MediaStoreImage並加入List
                    val image = MediaStoreImage(id, displayName, dateModified, contentUri)
                    images += image

                    // For debugging, we'll output the image objects we create to logcat.
                    Log.v(TAG, "Added image: $image")
                }
            }
        }

        Log.v(TAG, "Found ${images.size} images")
        return images
    }

    /**
     * Convenience method to convert a day/month/year date into a UNIX timestamp.
     *
     * We're suppressing the lint warning because we're not actually using the date formatter
     * to format the date to display, just to specify a format to use to parse it, and so the
     * locale warning doesn't apply.
     */
    @Suppress("SameParameterValue")
    @SuppressLint("SimpleDateFormat")
    private fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
        SimpleDateFormat("dd.MM.yyyy").let { formatter ->
            TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
        }

    /**
     * Convenience extension method to register a [ContentObserver] given a lambda.
     */
    private fun ContentResolver.registerObserver(
        uri: Uri,
        observer: (selfChange: Boolean) -> Unit
    ): ContentObserver {
        val contentObserver = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                observer(selfChange)
            }
        }
        //notifyForDescendents如果設為false，代表精準匹配，只要不等於設定的uri就不會通知
        //ex.設為為content://123，改變的為content://123/456，設為false的情況下就不會通知
        registerContentObserver(uri, true, contentObserver)
        return contentObserver
    }

    fun deleteImage(image: MediaStoreImage) {
        viewModelScope.launch {
            performDeleteImage(image)
        }
    }

    private suspend fun performDeleteImage(image: MediaStoreImage) {
        //跳到IO Thread
        withContext(Dispatchers.IO) {
            try {
                /**
                 * 在Android Q以上無法直接修改或刪除MediaStore的檔案，而且需要明確的permission
                 *
                 * In [Build.VERSION_CODES.Q] and above, it isn't possible to modify
                 * or delete items in MediaStore directly, and explicit permission
                 * must usually be obtained to do this.
                 *
                 * 這裡的執行方式是藉由捕捉[RecoverableSecurityException]，通過[IntentSender]讓activity可以
                 * 提示使用者給予權限來更新或刪除
                 *
                 * The way it works is the OS will throw a [RecoverableSecurityException],
                 * which we can catch here. Inside there's an [IntentSender] which the
                 * activity can use to prompt the user to grant permission to the item
                 * so it can be either updated or deleted.
                 */
                //嘗試透過Uri刪除檔案
                getApplication<Application>().contentResolver.delete(
                    image.contentUri,
                    "${MediaStore.Images.Media._ID} = ?",
                    arrayOf(image.id.toString())
                )
            } catch (securityException: SecurityException) {
                //Android Q以上時
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    //看是不是recoverableSecurityException，不是就拋出
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException
                            ?: throw securityException

                    // 向Activity發出信號，需要permission，如果成功將再次嘗試刪除
                    // Signal to the Activity that it needs to request permission and
                    // try the delete again if it succeeds.
                    pendingDeleteImage = image
                    _permissionNeededForDelete.postValue(
                        recoverableSecurityException.userAction.actionIntent.intentSender
                    )
                } else {
                    throw securityException
                }
            }
        }
    }
}