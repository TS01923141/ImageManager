package com.example.imagemanager.model.permission

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.example.imagemanager.R


//檢查跟要求Permission用的Fragment

/*
    Bundle傳進需要的permission
    onCreate用register機制要求permission
    失敗的話再次檢查，單一permission最多檢查三次
    最後返回要求成功還是失敗

    檢查流程
    組成permission list
    判斷list是否empty
    非empty就檢查first
    deniedCount設為0
    檢查true -> list刪掉第一個，判斷是否empty，繼續下一個
       false-> deniedCount計數，第一次顯示dialog，其他次數顯示再要求，三次沒過將permission加進deniedList
       ，並繼續檢查下一個，若全數檢查完畢，回傳結果
    檢查全部通過回傳true
    檢查有未通過回傳false跟dinedList
 */
private const val PERMISSION_LIST = "permission_list"
const val MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1
const val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 2

class PermissionRequestFragment : Fragment() {

    companion object {
        const val PERMISSION_REQUEST_RESULT = "permission_request_result"
        const val GRANTED_ALL_PERMISSION = "granted_all_permission"
        const val DENIED_LIST = "denied_list"
        const val FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
        const val WRITE_EXTERNAL_STORAGE = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        const val READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE
        const val MANAGE_EXTERNAL_STORAGE_PERMISSION = android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
        const val CAMERA = android.Manifest.permission.CAMERA
        //0-> no, 1 -> in setting view, 2 -> back this view
        var inSettingView = 0

        fun newInstance(permissionList: ArrayList<String>): PermissionRequestFragment {
            val f = PermissionRequestFragment()
            val args = Bundle()
            args.putStringArrayList(PERMISSION_LIST, permissionList)
            f.arguments = args
            return f
        }
    }

    private var currentCheckingPermission = ""
    private var requestPermissionList: MutableList<String> = mutableListOf()
    private var deniedList: ArrayList<String> = arrayListOf()
    private var deniedCount = 0

    //lifeCycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestPermissionList =
            arguments?.getStringArrayList(PERMISSION_LIST) ?: mutableListOf()
        if (requestPermissionList.isEmpty()) setResultAndFinish(true)
        checkAndRequestPermission(requestPermissionList.first())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //we don't need view.
        return null
    }

    override fun onResume() {
        super.onResume()
        if (inSettingView == 1) {
            inSettingView++
        } else if (inSettingView == 2) {
            inSettingView = 0
            checkAndRequestPermission(requestPermissionList.first())
        }
    }

    //permission

    fun checkAndRequestPermission(requestPermission: String) {
        currentCheckingPermission = requestPermission
        when {
            //check permission
            ContextCompat.checkSelfPermission(requireContext(), requestPermission) == PackageManager.PERMISSION_GRANTED -> {
                //granted permission
                requireNextOrFinish()
            }
            else -> {
                //not granted permission, request it
                requestPermissionLauncher.launch(requestPermission)
            }
        }
    }

    private fun requireNextOrFinish() {
        requestPermissionList.removeFirst()
        deniedCount = 0
        //if list is empty then finish, otherwise check next permission.
        if (requestPermissionList.isEmpty()) setResultAndFinish(deniedList.isEmpty())
        else checkAndRequestPermission(requestPermissionList.first())
    }

    private fun setResultAndFinish(result: Boolean) {
        //set request result.
        val bundle = bundleOf()
        bundle.putBoolean(GRANTED_ALL_PERMISSION , result)
        bundle.putStringArrayList(DENIED_LIST , deniedList)
        setFragmentResult(PERMISSION_REQUEST_RESULT, bundle)
        //finish itself.
        parentFragmentManager.beginTransaction().remove(this).commit()
    }

    private val requestPermissionLauncher =
        //request permission
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                //granted permission
                requireNextOrFinish()
            } else {
                //not granted permission, request again or finish
                deniedCount++
                if (deniedCount >= 3) {
                    //add permission to deniedList and check next
                    deniedList.add(currentCheckingPermission)
                    requireNextOrFinish()
                } else {
                    //first denied show dialog, other times request again.
                    if (deniedCount == 1) showExplainPermissionDialog(currentCheckingPermission)
                    else {
                        val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(requireActivity()
                            , currentCheckingPermission)
                        if (showRationale) {
                            checkAndRequestPermission(currentCheckingPermission)
                        } else {
                            openPermissionSettings(requireActivity())
                        }
                    }
                }
            }
        }

    private fun showExplainPermissionDialog(requestPermission: String) {
        //dialog that explain why need it
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setMessage(buildRequestMessage(requestPermission))
            //click negative button, add permission to deniedList and check next
            .setNegativeButton(
                requireContext().getString(R.string.no_thanks),
                DialogInterface.OnClickListener { dialogInterface, i ->
                    deniedList.add(currentCheckingPermission)
                    requireNextOrFinish()
                })
            //click positive button, request again
            .setPositiveButton(
                requireContext().getString(R.string.ok),
                DialogInterface.OnClickListener { dialogInterface, i ->
                    requestPermissionLauncher.launch(requestPermission)
                }).show()
    }

    private fun buildRequestMessage(permission: String): String {
        return when (permission) {
            FINE_LOCATION -> requireContext().getString(R.string.permission_explain_location)
            CAMERA -> requireContext().getString(R.string.permission_explain_camera)
            WRITE_EXTERNAL_STORAGE -> requireContext().getString(R.string.permission_explain_write_external_storage)
            READ_EXTERNAL_STORAGE -> requireContext().getString(R.string.permission_explain_read_external_storage)
            else -> ""
        }
    }

    //utils
    fun openPermissionSettings(activity: Activity) {
        inSettingView = 1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestStoragePermissionApi30()
        }
        else {
            activity.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.packageName, null)
                )
            )
        }
    }

    @RequiresApi(30)
    fun requestStoragePermissionApi30() {
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}.launch(intent)
//        startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST)
    }
}