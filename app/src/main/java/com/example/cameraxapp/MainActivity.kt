package com.example.cameraxapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.cameraxapp.databinding.ActivityMainBinding
import com.example.cameraxapp.util.PhotoPathUtil
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageCaptureBuilder: ImageCapture.Builder
    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null
    private var displayId = -1
    private var isCapturing: Boolean = false
    private var contentUri: Uri? = null

    private val cameraProviderFuture by lazy {
        ProcessCameraProvider.getInstance(this) // 카메라 얻어오면 이후 실행 리스너 등록
        // camerax는 수명주기 인식 해서 카메라 열고 닫는 작업 필요 없음
    }

    private val displayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private val cameraMainExecutors by lazy {
        ContextCompat.getMainExecutor(this)
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            if (this@MainActivity.displayId == displayId) {

            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this, R.layout.activity_main
        )
        setContentView(binding.root)

        // 카메라 권한 요청
        if (allPermissionsGranted()) {
            startCamera(binding.viewFinder)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

//        // photo 버튼 리스너 설정
//        binding.cameraCaptureButton.setOnClickListener {
//            takePhoto()
//        }

    }

    private fun bindCaptureListener() = with(binding) {
        cameraCaptureButton.setOnClickListener {
            if (!isCapturing) {
                isCapturing = true
                takePhoto()
            }
        }
    }

    private fun takePhoto() {
        if (::imageCapture.isInitialized.not()) return

        val photoFile = File(
            PhotoPathUtil.getOutputDirectory(this),
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.KOREA
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                    contentUri = savedUri
                    updateSavedImageContent()
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    isCapturing = false
                }

            })

    }

    private fun startCamera(viewFinder: PreviewView) {
        displayManager.registerDisplayListener(displayListener, null) // display가 가로 세로 변경 되었을 때 감지
        cameraExecutor = Executors.newSingleThreadExecutor()
        viewFinder.postDelayed({
            displayId = viewFinder.display.displayId
            bindCameraUseCase()
        }, 10)

    }

    private fun bindCameraUseCase() = with(binding) {
        val rotation = viewFinder.display.rotation //화면 회전 체크
        // 후방 카메라 기본값으로 선택
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build() //카메라 설정 (후면)

        cameraProviderFuture.addListener({
            // camera lifecycle 을 camera lifecycle owner 에 바인딩
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //프리뷰 설정
            val preview = Preview.Builder()
                .apply {
                    setTargetAspectRatio(AspectRatio.RATIO_4_3) // 비율 4:3
                    setTargetRotation(rotation) // 화면 로테이션 지정
//                    setTargetResolution(Size(200,200)) // 해상도 크기 지정할때 쓰임
                }.build()


            imageCaptureBuilder = ImageCapture.Builder() // 카메라 캡쳐 세팅
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // 지연을 최소화
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // 비율 4:3
                .setTargetRotation(rotation) // 화면 로테이션 지정
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)

            imageCapture = imageCaptureBuilder.build()


            try {
                // 리바인딩 전에 기존에 바인딩 되어있는 카메라 해제
                cameraProvider.unbindAll()
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this@MainActivity, cameraSelector, preview, imageCapture
                )
                preview.setSurfaceProvider(viewFinder.surfaceProvider)
                bindCaptureListener()
            } catch (exc: Exception) {
                Log.e("error", "error")
            }

        }, cameraMainExecutors)
    }

    private fun updateSavedImageContent() {
        contentUri?.let {
            isCapturing = try {
                val file = File(PhotoPathUtil.getPath(this, it) ?: throw FileNotFoundException())
                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(file.path),
                    arrayOf("image/jpeg"),
                    null
                ) // 외부에서 파일을 읽히도록 해줌
                false
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED // 앱에 권한이 있는지에 따라 PERMISSION_GRANTED 또는 PERMISSION_DENIED를 반환 체크
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(binding.viewFinder)
            } else { // 권한이 부여되지 않았을 때 알리는 토스트
                Toast.makeText(
                    this,
                    "카메라 권한이 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}