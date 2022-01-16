package com.harbinger.puzzlehost

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import com.harbinger.puzzlehost.dialog.MangaEditDialog
import com.harbinger.puzzlehost.dialog.OnEditResultListener
import com.harbinger.puzzlehost.utils.*
import com.harbinger.puzzlelibrary.OverrideUnityActivity
import com.unity3d.player.UnityPlayer
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.ArrayList

class MainActivity : OverrideUnityActivity(), EasyPermissions.PermissionCallbacks {
    private val TAG = "MainActivity"
    private var loadBar: ProgressDialog? = null
    private var pathList: ArrayList<String>? = ArrayList()
    private var index = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addControlsToUnityFrame()
        initProgressBar()
        doGetPaths()
    }

    override fun vibrate() {
        VibratorUtil.Vibrate(this, 50)
    }

    private fun initProgressBar() {
        loadBar = ProgressDialog(this)
        loadBar!!.setCancelable(false)
        loadBar!!.setMessage("稍等...")
    }

    @AfterPermissionGranted(111)
    private fun doGetPaths() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // Already have permission, do the thing
            // ...
            loadBar?.show()
            Thread(Runnable {
                pathList = FileSpider.getInstance().getFilteredImages(
                    this,
                    SharedPreferencesUtils.getSharedPreferencesData(this, ShareKeys.FILTER_KEY),
                    SharedPreferencesUtils.getSharedPreferencesData(this, ShareKeys.BLOCK_KEY)
                )
                runOnUiThread(Runnable {
                    loadBar?.dismiss()
                })
            }).start()
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this, "我们需要写入/读取权限",
                111, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun addControlsToUnityFrame() {
        val layout: FrameLayout = mUnityPlayer
        run {
            val myButton = Button(this)
            myButton.setText("Start")
            myButton.setX(20f)
            myButton.setY(20f)
            myButton.setOnClickListener {
                if (pathList == null || pathList!!.isEmpty() || index >= pathList!!.size) {
                    return@setOnClickListener
                }
                Log.d(TAG, "path${pathList?.get(index)}")
                UnityPlayer.UnitySendMessage(
                    "PuzzleManager",
                    "nextAndroidImage",
                    pathList?.get(index)
                )
                index++
            }
            myButton.setOnLongClickListener {
                showFilterDialog()
                return@setOnLongClickListener true
            }
            layout.addView(myButton, 200, 100)
        }
    }

    private fun showFilterDialog() {
        val dialog = MangaEditDialog(this)
        dialog.setOnEditResultListener(object : OnEditResultListener {
            override fun onResult(text: String?, text1: String?) {
                SharedPreferencesUtils.setSharedPreferencesData(
                    this@MainActivity,
                    ShareKeys.FILTER_KEY,
                    text
                )
                SharedPreferencesUtils.setSharedPreferencesData(
                    this@MainActivity,
                    ShareKeys.BLOCK_KEY,
                    text1
                )
            }

            override fun onCancelClick() {

            }
        })
        dialog.show()
        dialog.setTitle("筛选词设置")
        dialog.setHint("关键词以,分隔")
        dialog.setHint1("屏蔽词以，分隔")
        val filterText: String =
            SharedPreferencesUtils.getSharedPreferencesData(this, ShareKeys.FILTER_KEY)
        if (!TextUtils.isEmpty(filterText)) {
            dialog.setEditText(filterText)
        }
        val blockText: String =
            SharedPreferencesUtils.getSharedPreferencesData(this, ShareKeys.BLOCK_KEY)
        if (!TextUtils.isEmpty(blockText)) {
            dialog.setEditText1(blockText)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>?) {
        doGetPaths()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>?) {
        Toast.makeText(this, "......", Toast.LENGTH_LONG).show()
    }
}