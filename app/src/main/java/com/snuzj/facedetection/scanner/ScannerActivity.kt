package com.snuzj.facedetection.scanner

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
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
import com.snuzj.facedetection.CameraXViewModel
import com.snuzj.facedetection.databinding.ActivityScannerBinding
import java.util.concurrent.Executors

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding

    private lateinit var cameraSelector: CameraSelector
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis

    private val cameraXViewModel = viewModels<CameraXViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        cameraXViewModel.value.processCameraProvider.observe(this) { provider: ProcessCameraProvider? ->
            provider?.let {
                processCameraProvider = it
                bindCameraPreview()
                bindInputAnalyzer()
            }
        }

        binding.qrContentTv.setOnLongClickListener {
            copyTextToClipboard(binding.qrContentTv.text.toString())
            Toast.makeText(this,"Copied Text", Toast.LENGTH_SHORT).show()
            true // Indicate that we've handled the long-click
        }


    }

    // Function to copy text to the clipboard
    private fun copyTextToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Copied Text", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    private fun bindInputAnalyzer() {
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(barcodeScanner, imageProxy)
        }

        processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ){
        try {
            val inputImage = InputImage.fromMediaImage(
                imageProxy.image!!, imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        showBarcodeInfo(barcodes.first())
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

    @SuppressLint("SetTextI18n")
    private fun showBarcodeInfo(barcode: Barcode) {
        val rawValue = barcode.rawValue
        when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                binding.qrTypeTv.text = "URL"

                binding.qrContentTv.text = "$rawValue"
            }
            Barcode.TYPE_CONTACT_INFO -> {

                binding.qrTypeTv.text = "Contact Info"

                val typeContactInfo = barcode.contactInfo
                val name = "${typeContactInfo?.name?.formattedName}"
                val organization = "${typeContactInfo?.organization}"
                val title = "${typeContactInfo?.title}"

                var phones = ""
                typeContactInfo?.phones?.forEach { phone ->
                    phones += "\n${phone.number}"
                }

                var emails = ""
                typeContactInfo?.emails?.forEach { email ->
                    emails += "\n${email.address}"
                }

                binding.qrContentTv.text = "TYPE_CONTACT_INFO \nname: $name \norganization: $organization \ntitle: $title \nphones: $phones \nemails: $emails \n\n$rawValue"
            }
            Barcode.TYPE_EMAIL->{
                val typeEmail = barcode.email
                val address = "${typeEmail?.address}"
                val body = "${typeEmail?.body}"
                val subject = "${typeEmail?.subject}"

                binding.qrTypeTv.text = "email"
                binding.qrContentTv.text = "\naddress: $address \nbody: $body \nsubject: $subject \n\n$rawValue"
            }
            Barcode.TYPE_WIFI->{
                val typeWifi = barcode.wifi
                val ssid = "${typeWifi?.ssid}"
                val password = "${typeWifi?.password}"
                var encryptionType = "${typeWifi?.encryptionType}"

                when (encryptionType) {
                    "1" -> encryptionType = "OPEN"
                    "2" -> encryptionType = "WPA"
                    "3" -> encryptionType = "WEP"
                }
                binding.qrTypeTv.text = "Wifi"
                binding.qrContentTv.text = "\nssid: $ssid \npassword: $password \nencryptionType: $encryptionType \n \n$rawValue"
            }
            else -> {
                binding.qrTypeTv.text = "Other Type"
                binding.qrContentTv.text = barcode.rawValue.toString()
            }

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
        fun startScanner(context: Context, onScan: (barcodes: List<Barcode>) -> Unit) {
            onScan.invoke(emptyList()) // This is just a placeholder, make sure to handle it correctly.
            Intent(context, ScannerActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}

/*
    ML-Kit's Vision API on Android - Barcode Scanning
        step1: Creating a BarcodeScanner Object
        step2: Using Image analysis from CameraX
        step3: Reading Barcodes
 */