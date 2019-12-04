package com.soda1127.cameraxmlkit.ui.presenter

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.soda1127.cameraxmlkit.ui.MainActivity
import com.soda1127.cameraxmlkit.ui.view.MainView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import com.theartofdev.edmodo.cropper.CropImage
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import android.annotation.SuppressLint
import android.graphics.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import com.soda1127.cameraxmlkit.R


class MainPresenter(private val mainActivity: MainActivity) : MainView.PresenterView {

    private var permissionFlag = false

    var currentPhotoPath: String? = null

    init {
        onGettingPermission()
    }

    override fun grantPermission(): Boolean {
        return permissionFlag
    }

    override fun onCaptureClicked(CAMERA_REQUEST: Int) {
        if (!permissionFlag) {
            mainActivity.setToast(mainActivity.getString(R.string.denied))
            return
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (intent.resolveActivity(mainActivity.packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (e : IOException) { }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(mainActivity, "com.soda1127.cameraxmlkit.fileprovider", photoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                mainActivity.startActivityForResult(intent, CAMERA_REQUEST)
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = mainActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.absolutePath
        return image
    }

    override fun onGettingBitmapForImageView(bitmap: Bitmap) {
        mainActivity.showDialog()
        mainActivity.setBitmapOnImageView(bitmap)
        onGettingVisionBitmapAnalysis(bitmap)
    }

    override fun onGettingBitmapURIForCrop(bitmapURI: Uri) {
        CropImage.activity(bitmapURI)
            .setBackgroundColor(R.color.crop_shade)
            .setActivityTitle(R.string.cropping.toString())
            .start(mainActivity)
    }

    private fun onGettingVisionAnalysisText(bitmap: Bitmap, visionText: FirebaseVisionText) {

        val blocks = visionText.textBlocks

        mainActivity.setTextView(
            when {
                blocks.isEmpty() -> "No Text Found!!"
                else -> visionText.text
            }
        )

        if (blocks.isEmpty()) {
            mainActivity.dismissDialog()
            return
        }

        onGettingLabelFromImage(bitmap, blocks)

    }

    private fun onGettingVisionBitmapAnalysis(bitmap: Bitmap) {
        val resizedWidth = bitmap.width * (120f / bitmap.width)
        val resizedHeight = bitmap.height * (120f / bitmap.height)
        val resizedBmp = Bitmap.createScaledBitmap(bitmap, resizedWidth.toInt(), resizedHeight.toInt(), true)
        val fbVisionImg = FirebaseVisionImage.fromBitmap(resizedBmp)


        val options = FirebaseVisionCloudTextRecognizerOptions.Builder()
            .setLanguageHints(listOf("en", "ko"))
            .build()

        //val fbVisionTxtDetect = FirebaseVision.getInstance().getCloudTextRecognizer(options)
        val fbVisionTxtDetect = FirebaseVision.getInstance().onDeviceTextRecognizer
        fbVisionTxtDetect.processImage(fbVisionImg)
            .addOnSuccessListener {
                onGettingVisionAnalysisText(bitmap, it)
            }
            .addOnFailureListener {
                when {
                    it.printStackTrace().toString() == R.string.model_download_warning.toString() -> {
                        onGettingVisionBitmapAnalysis(bitmap)
                    }
                    else -> {
                        mainActivity.dismissDialog()
                        it.printStackTrace()
                    }
                }

            }
    }

    private fun onGettingLabelFromImage(bitmap: Bitmap, blocks: List<FirebaseVisionText.TextBlock>) {

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val graphics = onGettingGraphics()

        for (i in blocks.indices) {
            val lines: List<FirebaseVisionText.Line> = blocks[i].lines
            for (j in lines.indices) {
                val elements: List<FirebaseVisionText.Element> = lines[j].elements
                for (k in elements.indices) {
                    elements[k].boundingBox?.let { canvas.drawRect(it, graphics.first) }
                }
            }
        }

        mainActivity.setBitmapOnImageView(mutableBitmap)
        mainActivity.dismissDialog()

    }

    private fun onGettingPermission() {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        Permissions.check(mainActivity/*context*/, permissions, null/*options*/, null, object : PermissionHandler() {
            override fun onGranted() {
                permissionFlag = true
            }
        })
    }

    private fun onGettingGraphics(): Pair<Paint, Paint> {
        val rectPaint = Paint()
        rectPaint.color = Color.RED
        rectPaint.style = Paint.Style.STROKE
        rectPaint.strokeWidth = 4F

        val textPaint = Paint()
        textPaint.color = Color.RED
        textPaint.textSize = 40F

        return Pair(rectPaint, textPaint)
    }

}