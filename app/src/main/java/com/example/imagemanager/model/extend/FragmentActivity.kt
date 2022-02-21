package com.example.imagemanager.model.extend

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import com.example.imagemanager.model.permission.PermissionRequestFragment

fun FragmentActivity.checkAndRequestPermission(permissionList: ArrayList<String>, onResult: (Boolean) -> Unit) {
    supportFragmentManager.apply {
        //add listener
        setFragmentResultListener(PermissionRequestFragment.PERMISSION_REQUEST_RESULT, this@checkAndRequestPermission) {
                requestKey, bundle ->
            //show result on text view.
            val result = bundle.getBoolean(PermissionRequestFragment.GRANTED_ALL_PERMISSION)
            onResult.invoke(result)
        }

        //the list of permission that you want request or check.
        val requestFragment = PermissionRequestFragment.newInstance(permissionList)
        //start request permission
        beginTransaction()
            .add(android.R.id.content, requestFragment)
            .commit()
    }
}