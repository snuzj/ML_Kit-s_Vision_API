package com.snuzj.facedetection

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.snuzj.facedetection.databinding.ActivityMainBinding
import com.snuzj.facedetection.facedetection.FaceDetectionActivity
import com.snuzj.facedetection.scanner.ScannerActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val cameraPermission = Manifest.permission.CAMERA

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){isGranted->
        if (isGranted){
            startScanner()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.QRscanner.setOnClickListener {
            requestCameraAndStartScanner()
        }

        binding.faceDetectionBtn.setOnClickListener {
            FaceDetectionActivity.start(this)
        }



    }

    private fun requestCameraAndStartScanner(){
        if (isPermissionGranted(cameraPermission)){
            startScanner()
        } else{
            requestCameraPermission()
        }
    }

    private fun startScanner() {
        ScannerActivity.startScanner(this) {
        }
    }


    private fun requestCameraPermission() {
        when{
            shouldShowRequestPermissionRationale(cameraPermission) ->{
                cameraPermissionRequest {
                    openPermissionSetting()
                }
            }
            else -> {
                requestPermissionLauncher.launch(cameraPermission)
            }
        }
    }
}