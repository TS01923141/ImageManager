package com.example.imagemanager.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.imagemanager.R
import com.example.imagemanager.databinding.ActivityMainBinding
import com.example.imagemanager.model.permission.PermissionRequestFragment
import java.lang.StringBuilder

/*
    想做的功能
    抓所有圖片做成album
    透過FilePicker選圖
    透過URL複製圖片
    scan新圖
    從internal點圖片分享
    下載圖片
    檢查權限，如果是永遠拒絕的話跳setting頁

    --
    一個View配BottomBar
    bottomBar分所有圖片的album跟internal圖片album（兩個Fragment）
    internal右上角
     1.選圖可以透過FilePicker加圖進來
     2.dialog輸入網址下載圖片加圖進來

    所有圖片
    透過ContentProvider取得所有圖片

    RecyclerView用GridManager一排三張圖
    點擊可以分享
    --
    圖片嘗試用coil不用Glide
    分區儲存檢測
 */

/*
    Album -> 顯示手機內所有圖片，點圖分享
    MyGallery -> 選圖加到app內部資料夾，之後作為相簿圖片顯示，點圖刪除
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //set bottomBar and navController
        val navController = findNavController(R.id.navHostFrag_main)
        binding.navViewMain.setupWithNavController(navController)
    }

}