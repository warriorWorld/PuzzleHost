package com.harbinger.puzzlehost

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
import androidx.core.app.ActivityCompat.startActivityForResult

import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException


class MainActivity : OverrideUnityActivity(), EasyPermissions.PermissionCallbacks {
    private val TAG = "MainActivity"
    private val TAKE_PHOTO = 1 //声明一个请求码，用于识别返回的结果
    private var loadBar: ProgressDialog? = null
    private var pathList: ArrayList<String>? = ArrayList()
    private var index = 0
    private var imageUri: Uri? = null
    private val filePath: String =
        Environment.getExternalStorageDirectory().toString() + File.separator + "output_image.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addControlsToUnityFrame()
        initProgressBar()
        doGetPaths()
    }

    override fun vibrate() {
        VibratorUtil.Vibrate(this, 50)
    }

    override fun loadNextImage() {
        if (pathList == null || pathList!!.isEmpty() || index >= pathList!!.size) {
            return
        }
        Log.d(TAG, "path${pathList?.get(index)}")
        UnityPlayer.UnitySendMessage(
            "PuzzleManager",
            "nextAndroidImage",
            pathList?.get(index)
        )
        index++
    }

    @AfterPermissionGranted(222)
    override fun camera() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
            // Already have permission, do the thing
            // ...
            val outputImage = File(filePath)
            /*
                    创建一个File文件对象，用于存放摄像头拍下的图片，我们把这个图片命名为output_image.jpg
                    并把它存放在应用关联缓存目录下，调用getExternalCacheDir()可以得到这个目录，为什么要
                    用关联缓存目录呢？由于android6.0开始，读写sd卡列为了危险权限，使用的时候必须要有权限，
                    应用关联目录则可以跳过这一步
                     */
            try  //判断图片是否存在，存在则删除在创建，不存在则直接创建
            {
                if (!outputImage.parentFile.exists()) {
                    outputImage.parentFile.mkdirs()
                }
                if (outputImage.exists()) {
                    outputImage.delete()
                }
                outputImage.createNewFile()
                imageUri = if (Build.VERSION.SDK_INT >= 24) {
                    FileProvider.getUriForFile(
                        this,
                        "com.harbinger.puzzlehost.PhotoProvider", outputImage
                    )
                } else {
                    Uri.fromFile(outputImage)
                }
                //使用隐示的Intent，系统会找到与它对应的活动，即调用摄像头，并把它存储
                val intent = Intent("android.media.action.IMAGE_CAPTURE")
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(intent, TAKE_PHOTO)
                //调用会返回结果的开启方式，返回成功的话，则把它显示出来
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(
                this, "我们需要写入/读取权限",
                222, Manifest.permission.CAMERA
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TAKE_PHOTO -> if (resultCode === Activity.RESULT_OK) {
                val bp = CameraUtil.getBitmapWithRightRotation(filePath)
                FileSpider.saveBitmap(bp, filePath)
                UnityPlayer.UnitySendMessage(
                    "PuzzleManager",
                    "nextAndroidImage",
                    filePath
                )
            }
            else -> {
            }
        }
    }


    override fun showLoading() {
        runOnUiThread {
            loadBar?.show()
        }
    }

    override fun dismissLoading() {
        runOnUiThread {
            loadBar?.dismiss()
        }
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
            myButton.alpha = 0f
            myButton.setOnClickListener {
                loadNextImage();
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