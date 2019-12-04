package com.soda1127.cameraxmlkit.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.soda1127.cameraxmlkit.R
import com.soda1127.cameraxmlkit.baseDialog.BaseDialogPresenter
import com.soda1127.cameraxmlkit.ui.presenter.MainPresenter
import com.soda1127.cameraxmlkit.ui.view.MainView
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_main.*
import android.net.Uri
import java.io.File


class MainActivity : AppCompatActivity(), MainView.UIView {

    private val CAMERA_REQUEST = 1001
    private lateinit var presenter: MainPresenter
    private lateinit var baseDialog: BaseDialogPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Initialize Presenter
        presenter = MainPresenter(this@MainActivity)
        //Initialize Progress Presenter
        baseDialog = BaseDialogPresenter(this@MainActivity)

        //Setting Listeners
        settingListeners()
    }

    override fun setTextView(analyzedText: String) {
        imgTxtView.text = analyzedText
    }

    override fun setBitmapOnImageView(bitmap: Bitmap) {
        capturedImage.setImageBitmap(bitmap)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != CAMERA_REQUEST && resultCode != Activity.RESULT_OK) return

        when (requestCode) {

            /*CAMERA_REQUEST -> data?.data?.let {
                presenter.onGettingBitmapURIForCrop(it)
            }*/

            CAMERA_REQUEST -> {
                val file = File(presenter.currentPhotoPath)
                presenter.onGettingBitmapURIForCrop(Uri.fromFile(file))
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE ->
                presenter.onGettingBitmapForImageView(
                    MediaStore.Images.Media.getBitmap(
                        this.contentResolver,
                        CropImage.getActivityResult(data).uri
                    )
                )
        }
    }

    override fun setToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showDialog() {
        baseDialog.setAlertDialogView(true)
    }

    override fun dismissDialog() {
        baseDialog.setAlertDialogView(false)
    }

    private fun settingListeners() {
        btnOpenCamera.setOnClickListener {
            presenter.onCaptureClicked(CAMERA_REQUEST)
        }
    }

}