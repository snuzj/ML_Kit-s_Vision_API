package com.snuzj.facedetection.facedetection

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.snuzj.facedetection.CameraXViewModel
import com.snuzj.facedetection.R
import com.snuzj.facedetection.databinding.ActivityFaceDetectionBinding
import com.snuzj.facedetection.scanner.ScannerActivity
import java.util.concurrent.Executors

class FaceDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceDetectionBinding

    private val cameraXViewModel = viewModels<CameraXViewModel>()
    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var processCameraProvider: ProcessCameraProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

        cameraXViewModel.value.processCameraProvider.observe(this) { provider: ProcessCameraProvider? ->
            provider?.let {
                processCameraProvider = it
                bindCameraPreview()
                bindInputAnalyzer()
            }
        }
    }

    private fun bindInputAnalyzer() {

        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(detector, imageProxy)
        }

        processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        detector: FaceDetector,
        imageProxy: ImageProxy
    ){
        try {
            val inputImage = InputImage.fromMediaImage(
                imageProxy.image!!, imageProxy.imageInfo.rotationDegrees
            )

            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    binding.faceBoxOverlay.clear()
                    faces.forEach { face ->
                        val box = FaceBox(binding.faceBoxOverlay, face,imageProxy.cropRect)
                        binding.faceBoxOverlay.add(box)
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } catch (exception: Exception) {
            // Handle exceptions appropriately
            exception.printStackTrace()
            imageProxy.close()
        }
    }

    private fun bindCameraPreview(){
        cameraPreview = Preview.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)
        processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)

    }

    companion object{
        fun start(context: Context){
            Intent(context, FaceDetectionActivity::class.java).also{
                context.startActivity(it)
            }
        }
    }
}